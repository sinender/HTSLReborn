package llc.redstone.htslreborn.parser

import llc.redstone.systemsapi.data.Location
import guru.zoroark.tegral.niwen.lexer.Token
import llc.redstone.htslreborn.tokenizer.Tokenizer
import llc.redstone.htslreborn.tokenizer.Tokenizer.TokenWithPosition

object LocationParser {
    fun parse(str: String, iterator: Iterator<TokenWithPosition>): Location =
        when (str.lowercase().replace("_", " ")) {
            "house spawn" -> Location.HouseSpawn
            "current location" -> Location.CurrentLocation
            "invokers location" -> Location.InvokersLocation
            "custom coordinates" -> {
                val stringCoords = iterator.next().string
                val parts = stringCoords.split(" ")

                if (parts.size != 3 && parts.size != 5) {
                    error("Invalid custom location format: $stringCoords")
                }

                val xPart = parts[0]
                val yPart = parts[1]
                val zPart = parts[2]
                val pitchPart = parts.getOrNull(3)
                val yawPart = parts.getOrNull(4)

                val relX = xPart.startsWith("~")
                val relY = yPart.startsWith("~")
                val relZ = zPart.startsWith("~")
                val relPitch = pitchPart?.startsWith("~") ?: false
                val relYaw = yawPart?.startsWith("~") ?: false

                val x = xPart.removePrefix("~").toDoubleOrNull() ?: 0.0
                val y = yPart.removePrefix("~").toDoubleOrNull() ?: 0.0
                val z = zPart.removePrefix("~").toDoubleOrNull() ?: 0.0
                val pitch = pitchPart?.removePrefix("~")?.toFloatOrNull() ?: 0f
                val yaw = yawPart?.removePrefix("~")?.toFloatOrNull() ?: 0f

                Location.Custom(
                    relX = relX,
                    relY = relY,
                    relZ = relZ,
                    relPitch = relPitch,
                    relYaw = relYaw,
                    x = x,
                    y = y,
                    z = z,
                    pitch = pitch,
                    yaw = yaw,
                )
            }

            else -> error("Unknown location type: $str")
        }
}