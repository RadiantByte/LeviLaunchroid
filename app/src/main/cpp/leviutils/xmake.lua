add_rules("mode.debug", "mode.release")


target("leviutils")
    set_kind("shared")
    add_files("src/**.cpp")
    set_languages("c++20")
    add_packages("dobby")
    add_includedirs("src", {public = true})
    add_headerfiles("src/(**.h)")
    set_strip("all")
    add_links("android", "EGL", "GLESv3", "log")
    add_cxflags("-O3")
