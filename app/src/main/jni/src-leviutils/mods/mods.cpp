#include "mods.h"
#include <iostream>
#include <jni.h>

mods &mods::instance() {
  static mods instance;
  return instance;
}

std::string mods::get_mods_dir() const { return mods_dir_; }
std::string mods::get_mods_config_path() const { return mods_config_path_; }
void mods::set_mods_dir(const std::string &dir) { mods_dir_ = dir; }
void mods::set_mods_config_path(const std::string &path) {
  mods_config_path_ = path;
}
#include "logger/logger.h"

extern "C" JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_minecraft_MinecraftLauncher_nativeSetModPath(
    JNIEnv *env, jobject thiz, jstring modsDir, jstring configPath) {
  if (!modsDir || !configPath) {
    mods::instance().set_mods_dir("");
    mods::instance().set_mods_config_path("");
    return;
  }
  char mods_dir_c[256];
  char config_path_c[256];

  env->GetStringUTFRegion(modsDir, 0, env->GetStringLength(modsDir),
                          mods_dir_c);
  env->GetStringUTFRegion(configPath, 0, env->GetStringLength(configPath),
                          config_path_c);
  mods::instance().set_mods_dir(std::string(mods_dir_c));
  mods::instance().set_mods_config_path(std::string(config_path_c));
}