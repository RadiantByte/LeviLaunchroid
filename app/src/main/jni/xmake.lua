add_rules("mode.debug", "mode.release")

add_requires("nlohmann_json v3.11.3")
add_requires("dobby")
add_requires("opengl-headers")


target("preloader")
    set_kind("shared")
    add_files("src-preloader/**.cpp")
    add_includedirs("src-preloader")
    add_packages("nlohmann_json")
    add_deps("leviutils")

target("leviutils")
    set_kind("shared")
    add_files("src-leviutils/**.cpp")
    set_languages("c++20")
    add_packages("dobby")
    add_includedirs("src-leviutils", {public = true})
    add_headerfiles("src-leviutils/(**.h)")
    set_strip("all")
    add_links("android", "EGL", "GLESv3", "log")
    add_cxflags("-O3")
