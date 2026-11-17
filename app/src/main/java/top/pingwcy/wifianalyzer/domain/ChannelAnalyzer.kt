package top.pingwcy.wifianalyzer.domain

import top.pingwcy.wifianalyzer.model.WifiData

object ChannelAnalyzer {

    fun analyze(
        list: List<WifiData>,
        band: String?,
        useOccupied: Boolean = true,
        showEmpty: Boolean = false,
        ignoreWeak: Boolean = false,
        weakThreshold: Int = -82
    ): List<ChannelUsage> {
        val filtered = (if (band == null) list else list.filter { it.band == band })
            .filter { !ignoreWeak || it.rssi >= weakThreshold }

        // channel -> 该信道受到的加权贡献（一个 AP 最多对同一信道贡献一条记录）
        val map = mutableMapOf<Int, MutableList<WeightedWifi>>()
        filtered.forEach { wifi ->
            val contributions: Map<Int, Double> = if (useOccupied) {
                ChannelCalculator.interferenceWeights(wifi)
            } else {
                mapOf(wifi.primaryChannel to 1.0)
            }
            contributions.forEach { (ch, weight) ->
                if (ch > 0 && weight > 0.0) {
                    map.getOrPut(ch) { mutableListOf() }.add(WeightedWifi(wifi, weight))
                }
            }
        }

        if (showEmpty) {
            val bandsToFill = if (band == null) listOf("2.4 GHz", "5 GHz", "6 GHz") else listOf(band)
            bandsToFill.forEach { b ->
                allChannelsOfBand(b).forEach { ch -> map.getOrPut(ch) { mutableListOf() } }
            }
        }

        return map.entries.sortedBy { it.key }
            .map { (ch, list) ->
                ChannelUsage(
                    channel = ch,
                    count = list.size,
                    load = list.sumOf { it.weight },
                    networks = list.map { it.wifi.ssid },
                    score = ChannelScorer.scoreChannel(ch, list)
                )
            }
    }

    fun allChannelsOfBand(band: String): List<Int> = when (band) {
        "2.4 GHz" -> (1..14).toList()
        "5 GHz" -> listOf(
            36, 40, 44, 48, 52, 56, 60, 64,
            100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144,
            149, 153, 157, 161, 165, 169, 173, 177
        )
        "6 GHz" -> (1..233 step 4).toList()
        else -> emptyList()
    }
}
