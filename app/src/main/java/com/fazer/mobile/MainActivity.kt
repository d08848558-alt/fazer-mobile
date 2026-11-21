package com.fazer.mobile

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : Activity() {

    private val CONFIG_URL = "https://raw.githubusercontent.com/d08848558-alt/fazer-mobile/main/server-files/launcher.json"

    private val BG = 0xFF0A0A0E.toInt()
    private val CARD = 0xFF18191E.toInt()
    private val BORDER = 0xFF2C2D33.toInt()
    private val RED = 0xFFBA1C2E.toInt()
    private val GREY = 0xFF9A9BA3.toInt()

    private lateinit var dot: View
    private lateinit var serverSub: TextView
    private lateinit var newsSub: TextView
    private lateinit var updSub: TextView
    private var cfg: JSONObject? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun bg(color: Int, r: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(r).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun text(s: String, sp: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = sp; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun card(title: String, sub: String): Pair<LinearLayout, TextView> {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = bg(CARD, 20, BORDER)
            setPadding(dp(20), dp(18), dp(20), dp(18))
        }
        c.addView(text(title, 20f, Color.WHITE, true))
        val s = text(sub, 14f, GREY).apply { setPadding(0, dp(8), 0, 0) }
        c.addView(s)
        val lp = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) }
        c.layoutParams = lp
        return c to s
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG) }
        root.addView(View(this).apply { setBackgroundColor(RED); layoutParams = LinearLayout.LayoutParams(-1, dp(6)) })

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(32), dp(24), dp(24))
        }
        col.addView(text("FAZER", 44f, Color.WHITE, true))
        col.addView(text("MOBILE", 44f, 0xFFE0263A.toInt(), true))
        col.addView(text("ROLEPLAY • ANDROID", 15f, GREY).apply { setPadding(0, dp(8), 0, dp(24)) })

        val (srv, ssub) = card("СЕРВЕР", "Подключение ещё не настроено")
        serverSub = ssub
        dot = View(this).apply { background = bg(0xFFE0A63A.toInt(), 20); layoutParams = LinearLayout.LayoutParams(dp(12), dp(12)).apply { rightMargin = dp(10) } }
        val head = srv.getChildAt(0) as TextView
        srv.removeViewAt(0)
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(dot); row.addView(head)
        srv.addView(row, 0)
        col.addView(srv)

        val play = text("ИГРАТЬ", 20f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = bg(RED, 20)
            layoutParams = LinearLayout.LayoutParams(-1, dp(64)).apply { topMargin = dp(24); bottomMargin = dp(8) }
            setOnClickListener { play() }
        }
        col.addView(play)

        val (n, nsub) = card("НОВОСТИ", "Здесь будут новости FAZER MOBILE"); newsSub = nsub; col.addView(n)
        val (u, usub) = card("ОБНОВЛЕНИЯ", "Версия клиента пока не установлена"); updSub = usub; col.addView(u)
        val (s, _) = card("НАСТРОЙКИ", "Графика • Звук • Уведомления")
        s.setOnClickListener { Toast.makeText(this, "Скоро", Toast.LENGTH_SHORT).show() }
        col.addView(s)

        root.addView(ScrollView(this).apply { addView(col) })
        setContentView(root)
        load()
    }

    private fun load() = Thread {
        try {
            val json = JSONObject(URL(CONFIG_URL).readText())
            cfg = json
            val sv = json.getJSONObject("server")
            val name = sv.getString("name")
            val info = try { query(sv.getString("ip"), sv.getInt("port")) } catch (e: Exception) { null }
            val news = json.optJSONArray("news")?.let { a -> (0 until a.length()).joinToString("\n• ", "• ") { a.getString(it) } }
            val latest = json.optString("client_version", "?")
            val installed = try { packageManager.getPackageInfo(json.getString("client_package"), 0).versionName } catch (e: Exception) { null }
            runOnUiThread {
                serverSub.text = if (info != null) "$name — онлайн ${info.first}/${info.second}" else "$name — офлайн"
                dot.background = bg(if (info != null) 0xFF2ECC71.toInt() else 0xFFE0263A.toInt(), 20)
                if (!news.isNullOrEmpty()) newsSub.text = news
                updSub.text = when {
                    installed == null -> "Клиент не установлен. Доступна версия $latest"
                    installed != latest -> "Установлена $installed, доступна $latest"
                    else -> "Актуальная версия $latest"
                }
            }
        } catch (e: Exception) {
            runOnUiThread { serverSub.text = "Не удалось получить настройки" }
        }
    }.start()

    private fun query(host: String, port: Int): Pair<Int, Int> {
        DatagramSocket().use { s ->
            s.soTimeout = 3000
            val addr = InetAddress.getByName(host)
            val pkt = "SAMP".toByteArray() + addr.address +
                byteArrayOf((port and 0xFF).toByte(), ((port shr 8) and 0xFF).toByte(), 'i'.code.toByte())
            s.send(DatagramPacket(pkt, pkt.size, addr, port))
            val buf = ByteArray(512)
            s.receive(DatagramPacket(buf, buf.size))
            val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
            return (bb.getShort(12).toInt() and 0xFFFF) to (bb.getShort(14).toInt() and 0xFFFF)
        }
    }

    private fun play() {
        val c = cfg ?: return Toast.makeText(this, "Настройки ещё не загружены", Toast.LENGTH_SHORT).show()
        val launch = packageManager.getLaunchIntentForPackage(c.getString("client_package"))
        if (launch != null) startActivity(launch)
        else {
            Toast.makeText(this, "Клиент не установлен", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(c.getString("client_url"))))
        }
    }
}
