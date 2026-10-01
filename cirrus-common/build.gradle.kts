plugins {
    id("java")
}

group = "gg.modl.minecraft.cirrus"
version = "4.2.4"

dependencies {
    implementation(project(":cirrus-api"))
    compileOnly("com.github.retrooper:packetevents-api:2.14.0")
    compileOnly("net.kyori:adventure-api:4.17.0")

    testImplementation("com.github.retrooper:packetevents-api:2.14.0")
    testImplementation("net.kyori:adventure-api:4.14.0")
    testImplementation("net.kyori:adventure-nbt:4.14.0")
    testImplementation("org.jspecify:jspecify:1.0.0")
}
