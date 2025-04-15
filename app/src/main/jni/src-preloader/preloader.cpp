#include <dlfcn.h>
#include <filesystem>
#include <fstream>
#include <jni.h>
#include <memory>
#include <string>

#include <android/native_activity.h>

#include "logger/logger.h"
#include "nlohmann/json.hpp"

using json = nlohmann::json;

namespace fs = std::filesystem;

auto *logger = new Logger("LeviMC");

static void (*android_main_minecraft)(struct android_app *app);
static void (*ANativeActivity_onCreate_minecraft)(ANativeActivity *activity,
                                                  void *savedState,
                                                  size_t savedStateSize);

extern "C" void android_main(struct android_app *app) {
  android_main_minecraft(app);
}

extern "C" void ANativeActivity_onCreate(ANativeActivity *activity,
                                         void *savedState,
                                         size_t savedStateSize) {
  ANativeActivity_onCreate_minecraft(activity, savedState, savedStateSize);
}

namespace {
struct AndroidContext {
  std::string internal_storage_path;
  std::string package_name;
  std::string data_dir;
  std::string mods_dir;
};

std::unique_ptr<AndroidContext> g_ctx;

bool ends_with_so(const std::string &filename) {
  constexpr size_t suffix_len = 3;
  if (filename.length() < suffix_len)
    return false;
  return filename.substr(filename.length() - suffix_len) == ".so";
}

void copy_file_faster(const fs::path &src, const fs::path &dst) {
  std::ifstream in(src, std::ios::binary);
  std::ofstream out(dst, std::ios::binary);
  out << in.rdbuf();
}

jobject get_global_context(JNIEnv *env) {
  jclass activity_thread = env->FindClass("android/app/ActivityThread");
  jmethodID current_activity_thread =
      env->GetStaticMethodID(activity_thread, "currentActivityThread",
                             "()Landroid/app/ActivityThread;");
  jobject at =
      env->CallStaticObjectMethod(activity_thread, current_activity_thread);
  jmethodID get_application = env->GetMethodID(
      activity_thread, "getApplication", "()Landroid/app/Application;");
  jobject context = env->CallObjectMethod(at, get_application);
  if (env->ExceptionCheck())
    env->ExceptionClear();
  return context;
}

std::string get_absolute_path(JNIEnv *env, jobject file) {
  jclass file_class = env->GetObjectClass(file);
  jmethodID get_abs_path =
      env->GetMethodID(file_class, "getAbsolutePath", "()Ljava/lang/String;");
  auto jstr = (jstring)env->CallObjectMethod(file, get_abs_path);
  if (env->ExceptionCheck())
    env->ExceptionClear();
  const char *cstr = env->GetStringUTFChars(jstr, nullptr);
  std::string result(cstr);
  env->ReleaseStringUTFChars(jstr, cstr);
  return result;
}

std::string get_package_name(JNIEnv *env, jobject context) {
  jclass context_class = env->GetObjectClass(context);
  jmethodID get_pkg_name =
      env->GetMethodID(context_class, "getPackageName", "()Ljava/lang/String;");
  auto jstr = (jstring)env->CallObjectMethod(context, get_pkg_name);
  if (env->ExceptionCheck())
    env->ExceptionClear();
  const char *cstr = env->GetStringUTFChars(jstr, nullptr);
  std::string result(cstr);
  env->ReleaseStringUTFChars(jstr, cstr);
  return result;
}

void initialize_context(JNIEnv *env) {
  auto ctx = std::make_unique<AndroidContext>();

  jobject app_context = get_global_context(env);
  if (!app_context) {
    return;
  }

  jclass context_class = env->GetObjectClass(app_context);
  jmethodID get_files_dir =
      env->GetMethodID(context_class, "getFilesDir", "()Ljava/io/File;");
  jobject files_dir = env->CallObjectMethod(app_context, get_files_dir);
  ctx->data_dir = get_absolute_path(env, files_dir);

  jclass env_class = env->FindClass("android/os/Environment");
  jmethodID get_storage_dir = env->GetStaticMethodID(
      env_class, "getExternalStorageDirectory", "()Ljava/io/File;");
  jobject storage_dir = env->CallStaticObjectMethod(env_class, get_storage_dir);
  ctx->internal_storage_path = get_absolute_path(env, storage_dir);

  ctx->package_name = get_package_name(env, app_context);
  for (auto &c : ctx->package_name)
    c = tolower(c);

  // ctx->mods_dir = (fs::path(ctx->internal_storage_path) / "Android" / "data"
  // / ctx->package_name / "mods");
  ctx->mods_dir =
      (fs::path(ctx->internal_storage_path) / "games" / "org.levimc" / "mods");
  fs::create_directories(ctx->mods_dir);

  // LOG("Initialized context for package: %s", ctx->package_name.c_str());
  g_ctx = std::move(ctx);
}

bool is_mod_enabled(const fs::path &json_path,
                    const std::string &mod_filename) {
  if (!fs::exists(json_path))
    return false;
  std::ifstream json_file(json_path);
  if (!json_file)
    return false;
  json mods_config;
  json_file >> mods_config;
  if (mods_config.contains(mod_filename)) {
    return mods_config[mod_filename].get<bool>();
  }
  return false;
}

void load_mods(const fs::path &mods_dir, const fs::path &target_dir) {
  if (!fs::exists(mods_dir))
    return;
  // LOG("Load mods: %s", mods_dir.c_str());
  for (const auto &entry : fs::directory_iterator(mods_dir)) {
    if (entry.is_regular_file() &&
        ends_with_so(entry.path().filename().string())) {
      fs::path json_path = fs::path(g_ctx->mods_dir) / "mods_config.json";

      auto dest_path = target_dir / entry.path().filename();
      if (!is_mod_enabled(json_path, entry.path().filename().c_str()))
        continue;
      fs::copy_file(entry.path(), dest_path,
                    fs::copy_options::overwrite_existing);
      fs::permissions(dest_path, fs::perms::owner_all | fs::perms::group_all);

      if (auto handle = dlopen(dest_path.c_str(), RTLD_NOW)) {
        logger->info("Loaded mod: %s", entry.path().filename().c_str());
      } else {
        logger->error("Failed to load %s: %s", entry.path().filename().c_str(),
                      dlerror());
      }
    }
  }
}
} // namespace

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
  void *handle = dlopen("libminecraftpe.so", RTLD_LAZY);
  android_main_minecraft =
      (void (*)(struct android_app *))(dlsym(handle, "android_main"));
  ANativeActivity_onCreate_minecraft =
      (void (*)(ANativeActivity *, void *, size_t))(
          dlsym(handle, "ANativeActivity_onCreate"));
  JNIEnv *env = nullptr;
  if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return JNI_VERSION_1_6;
  }
  initialize_context(env);
  if (!g_ctx) {
    return JNI_VERSION_1_6;
  }

  load_mods(g_ctx->mods_dir, g_ctx->data_dir);
  return JNI_VERSION_1_6;
}