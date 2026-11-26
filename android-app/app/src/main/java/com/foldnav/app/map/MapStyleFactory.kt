package com.foldnav.app.map

import com.foldnav.app.model.MapTheme
import org.json.JSONArray
import org.json.JSONObject

/**
 * Generates custom MapLibre Style v8 JSON documents wired to OpenFreeMap's free
 * OpenMapTiles vector endpoint, including 3D building extrusions and glow highways.
 */
object MapStyleFactory {
    private const val TILEJSON_URL = "https://tiles.openfreemap.org/planet"
    private const val GLYPHS_URL = "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf"

    fun buildStyleJson(theme: MapTheme): String {
        val root = JSONObject()
        root.put("version", 8)
        root.put("name", theme.title)
        root.put("glyphs", GLYPHS_URL)

        val light = JSONObject().apply {
            put("anchor", "viewport")
            put("color", "#FFFFFF")
            put("intensity", 0.45)
            put("position", JSONArray(listOf(1.15, 210, 32)))
        }
        root.put("light", light)

        val openMapTilesSource = JSONObject().apply {
            put("type", "vector")
            put("url", TILEJSON_URL)
        }
        val sources = JSONObject().apply {
            put("openmaptiles", openMapTilesSource)
        }
        root.put("sources", sources)

        val layers = JSONArray()

        // 1. Deep AMOLED / Obsidian background
        layers.put(JSONObject().apply {
            put("id", "background")
            put("type", "background")
            put("paint", JSONObject().put("background-color", theme.bgHex))
        })

        // 2. Urban landuse subtle tint
        layers.put(JSONObject().apply {
            put("id", "landuse_urban")
            put("type", "fill")
            put("source", "openmaptiles")
            put("source-layer", "landuse")
            put("paint", JSONObject().apply {
                put("fill-color", theme.landuseHex)
                put("fill-opacity", 0.65)
            })
        })

        // 3. Parks & foliage
        layers.put(JSONObject().apply {
            put("id", "park")
            put("type", "fill")
            put("source", "openmaptiles")
            put("source-layer", "park")
            put("paint", JSONObject().apply {
                put("fill-color", theme.parkHex)
                put("fill-opacity", 0.78)
            })
        })

        // 4. Water bodies & rivers
        layers.put(JSONObject().apply {
            put("id", "water")
            put("type", "fill")
            put("source", "openmaptiles")
            put("source-layer", "water")
            put("paint", JSONObject().put("fill-color", theme.waterHex))
        })

        layers.put(JSONObject().apply {
            put("id", "waterway")
            put("type", "line")
            put("source", "openmaptiles")
            put("source-layer", "waterway")
            put("paint", JSONObject().apply {
                put("line-color", theme.waterHex)
                put("line-width", 2.0)
            })
        })

        // 5. Minor / service roads
        layers.put(JSONObject().apply {
            put("id", "highway_minor")
            put("type", "line")
            put("source", "openmaptiles")
            put("source-layer", "transportation")
            put("minzoom", 11)
            put("filter", JSONArray("""["match", ["get", "class"], ["minor", "service", "track"], true, false]"""))
            put("layout", JSONObject().apply {
                put("line-cap", "round")
                put("line-join", "round")
            })
            put("paint", JSONObject().apply {
                put("line-color", theme.minorRoadHex)
                put("line-width", JSONArray("""["interpolate", ["linear"], ["zoom"], 12, 0.8, 16, 4.0, 18, 8.0]"""))
                put("line-opacity", 0.85)
            })
        })

        // 6. Major arterials with soft glow + core
        val majorFilter = JSONArray("""["match", ["get", "class"], ["primary", "secondary", "tertiary", "trunk"], true, false]""")
        layers.put(JSONObject().apply {
            put("id", "highway_major_glow")
            put("type", "line")
            put("source", "openmaptiles")
            put("source-layer", "transportation")
            put("filter", majorFilter)
            put("layout", JSONObject().apply {
                put("line-cap", "round")
                put("line-join", "round")
            })
            put("paint", JSONObject().apply {
                put("line-color", theme.majorRoadHex)
                put("line-width", JSONArray("""["interpolate", ["linear"], ["zoom"], 8, 1.8, 14, 7.5, 17, 13.0]"""))
                put("line-blur", 3.5)
                put("line-opacity", 0.34)
            })
        })

        layers.put(JSONObject().apply {
            put("id", "highway_major")
            put("type", "line")
            put("source", "openmaptiles")
            put("source-layer", "transportation")
            put("filter", majorFilter)
            put("layout", JSONObject().apply {
                put("line-cap", "round")
                put("line-join", "round")
            })
            put("paint", JSONObject().apply {
                put("line-color", theme.majorRoadHex)
                put("line-width", JSONArray("""["interpolate", ["linear"], ["zoom"], 8, 0.9, 14, 3.2, 17, 5.8]"""))
                put("line-opacity", 0.95)
            })
        })

        // 7. Motorways & expressways with bright neon halo + core
        val motorwayFilter = JSONArray("""["==", ["get", "class"], "motorway"]""")
        layers.put(JSONObject().apply {
            put("id", "highway_motorway_glow")
            put("type", "line")
            put("source", "openmaptiles")
            put("source-layer", "transportation")
            put("filter", motorwayFilter)
            put("layout", JSONObject().apply {
                put("line-cap", "round")
                put("line-join", "round")
            })
            put("paint", JSONObject().apply {
                put("line-color", theme.motorwayHex)
                put("line-width", JSONArray("""["interpolate", ["linear"], ["zoom"], 6, 2.2, 13, 10.0, 17, 16.0]"""))
                put("line-blur", 4.0)
                put("line-opacity", 0.42)
            })
        })

        layers.put(JSONObject().apply {
            put("id", "highway_motorway")
            put("type", "line")
            put("source", "openmaptiles")
            put("source-layer", "transportation")
            put("filter", motorwayFilter)
            put("layout", JSONObject().apply {
                put("line-cap", "round")
                put("line-join", "round")
            })
            put("paint", JSONObject().apply {
                put("line-color", theme.motorwayHex)
                put("line-width", JSONArray("""["interpolate", ["linear"], ["zoom"], 6, 1.0, 13, 4.2, 17, 7.2]"""))
            })
        })

        // 8. 3D Extruded Buildings (sculpted with directional viewport light)
        layers.put(JSONObject().apply {
            put("id", "building_3d")
            put("type", "fill-extrusion")
            put("source", "openmaptiles")
            put("source-layer", "building")
            put("minzoom", 13)
            put("filter", JSONArray("""["!", ["has", "hide_3d"]]"""))
            put("paint", JSONObject().apply {
                put("fill-extrusion-color", theme.buildingHex)
                put("fill-extrusion-height", JSONArray("""["coalesce", ["get", "render_height"], 12]"""))
                put("fill-extrusion-base", JSONArray("""["coalesce", ["get", "render_min_height"], 0]"""))
                put("fill-extrusion-opacity", 0.88)
            })
        })

        // 9. Street & Place Labels using OpenFreeMap Noto Sans Regular glyphs
        layers.put(JSONObject().apply {
            put("id", "road_labels")
            put("type", "symbol")
            put("source", "openmaptiles")
            put("source-layer", "transportation_name")
            put("minzoom", 13)
            put("layout", JSONObject().apply {
                put("symbol-placement", "line")
                put("text-field", JSONArray("""["coalesce", ["get", "name:en"], ["get", "name"]]"""))
                put("text-font", JSONArray(listOf("Noto Sans Regular")))
                put("text-size", 11.5)
            })
            put("paint", JSONObject().apply {
                put("text-color", theme.labelHex)
                put("text-halo-color", theme.bgHex)
                put("text-halo-width", 1.5)
            })
        })

        layers.put(JSONObject().apply {
            put("id", "place_labels")
            put("type", "symbol")
            put("source", "openmaptiles")
            put("source-layer", "place")
            put("minzoom", 6)
            put("layout", JSONObject().apply {
                put("text-field", JSONArray("""["coalesce", ["get", "name:en"], ["get", "name"]]"""))
                put("text-font", JSONArray(listOf("Noto Sans Regular")))
                put("text-size", JSONArray("""["interpolate", ["linear"], ["zoom"], 6, 11, 14, 15]"""))
            })
            put("paint", JSONObject().apply {
                put("text-color", theme.labelHex)
                put("text-halo-color", theme.bgHex)
                put("text-halo-width", 2.0)
            })
        })

        root.put("layers", layers)
        return root.toString()
    }
}
