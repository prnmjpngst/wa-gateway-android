package com.aji.wa_gateway.service

import com.aji.wa_gateway.db.entity.Target
import com.aji.wa_gateway.util.DateUtil

object MessageGenerator {
    fun generate(template: String, target: Target): String {
        val hitungHari = DateUtil.calculateHitungHari(target.masaBerlaku)
        return template
            .replace("{nama}", target.namaPemilik.ifBlank { "-" })
            .replace("{nomor_kendaraan}", target.nomorKendaraan.ifBlank { "-" })
            .replace("{masa_berlaku}", target.masaBerlaku.ifBlank { "-" })
            .replace("{hitung_hari}", hitungHari.toString())
    }

    fun generateForTargets(template: String, targets: List<Target>): List<Target> {
        return targets.map { target ->
            target.copy(pesan = generate(template, target))
        }
    }
}
