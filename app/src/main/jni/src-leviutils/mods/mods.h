#pragma once
#include <string>

class mods {
public:
  mods(const mods &) = delete;
  mods &operator=(const mods &) = delete;

  static mods &instance();

  std::string get_mods_dir() const;
  std::string get_mods_config_path() const;
  void set_mods_dir(const std::string &dir);
  void set_mods_config_path(const std::string &path);

private:
  mods() = default;

  std::string mods_dir_;
  std::string mods_config_path_;
};