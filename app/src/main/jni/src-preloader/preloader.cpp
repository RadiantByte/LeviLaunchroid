#include "logger/logger.h"
#include "mods/mods.h"
#include "nlohmann/json.hpp"
#include <android/native_activity.h>
#include <dlfcn.h>
#include <filesystem>
#include <fstream>
#include <jni.h>
#include <memory>
#include <string>

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

bool ends_with_so(const std::string &filename) {
  return filename.size() >= 3 && filename.substr(filename.size() - 3) == ".so";
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

void load_mods(const std::string &mods_dir, const std::string &mods_config_path,
               const std::string &data_dir) {
  logger->info("Loading mods from %s", mods_dir.c_str());
  if (!fs::exists(mods_dir))
    return;

  for (const auto &entry : fs::directory_iterator(mods_dir)) {
    if (entry.is_regular_file() &&
        ends_with_so(entry.path().filename().string())) {
      const auto &mod_filename = entry.path().filename().string();
      if (!is_mod_enabled(mods_config_path, mod_filename))
        continue;

      fs::path dest_path = fs::path(data_dir) / mod_filename;
      fs::copy_file(entry.path(), dest_path,
                    fs::copy_options::overwrite_existing);
      fs::permissions(dest_path, fs::perms::owner_all | fs::perms::group_all);
      if (auto handle = dlopen(dest_path.c_str(), RTLD_NOW)) {
        logger->info("Loaded mod: %s", mod_filename.c_str());
      } else {
        logger->error("Failed to load %s: %s", mod_filename.c_str(), dlerror());
      }
    }
  }
}

void my_mods_init_hook(std::string data_dir) {
  if (!mods::instance().get_mods_dir().empty() &&
      !mods::instance().get_mods_config_path().empty()) {
    load_mods(mods::instance().get_mods_dir(),
              mods::instance().get_mods_config_path(), data_dir);
  } else {
    logger->error(
        "Mod path/config path/target dir is not set, cannot load mods now.");
  }
}

} // namespace

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

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *) {
  void *handle = dlopen("libminecraftpe.so", RTLD_LAZY);
  android_main_minecraft =
      (void (*)(struct android_app *))(dlsym(handle, "android_main"));
  ANativeActivity_onCreate_minecraft =
      (void (*)(ANativeActivity *, void *, size_t))(
          dlsym(handle, "ANativeActivity_onCreate"));
  logger->info("[JNI] LeviMC native loaded.");

  JNIEnv *env = nullptr;
  if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return JNI_VERSION_1_6;
  }
  jobject app_context = get_global_context(env);
  if (!app_context) {
    return JNI_VERSION_1_6;
  }
  jclass context_class = env->GetObjectClass(app_context);
  jmethodID get_files_dir =
      env->GetMethodID(context_class, "getFilesDir", "()Ljava/io/File;");
  jobject files_dir = env->CallObjectMethod(app_context, get_files_dir);
  std::string data_dir = get_absolute_path(env, files_dir);

  my_mods_init_hook(data_dir);
  return JNI_VERSION_1_6;
}