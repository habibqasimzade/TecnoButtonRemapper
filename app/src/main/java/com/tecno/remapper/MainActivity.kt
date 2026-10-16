package com.tecno.remapper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private lateinit var statusPerm1: TextView
    private lateinit var statusPerm2: TextView
    private lateinit var statusPerm3: TextView
    private lateinit var actionPermButton: Button
    private lateinit var settingsContainer: LinearLayout

    private lateinit var spinnerVolDown: Spinner
    private lateinit var spinnerVolUp: Spinner
    private lateinit var spinnerPower: Spinner

    // Seçilə bilən funksiyalar və onların daxili kod adları
    private val actionLabels = arrayOf(
        "Heç nə etmə (Deaktiv)",
        "Fənəri Aç / Bağla (Flashlight)",
        "Google Pay (Wallet) Aç",
        "Kameranı Aç",
        "WhatsApp-ı Aç",
        "Səssiz / Normal Rejim (Mute)"
    )

    private val actionCodes = arrayOf(
        "NONE",
        "FLASHLIGHT",
        "GOOGLE_PAY",
        "CAMERA",
        "WHATSAPP",
        "MUTE"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("RemapPrefs", Context.MODE_PRIVATE)

        // Əsas Ekran Dizaynı (Kod ilə təmiz və sürətli)
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F5F7FA"))
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        // --- BÖLMƏ 1: İCAZƏLƏRİN ADDIM-ADDIM YOXLANILMASI ---
        val permCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(40, 40, 40, 40)
            elevation = 8f
        }

        val permTitle = TextView(this).apply {
            text = "1. Sistem İcazələri (Məcburi)"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1A1A"))
        }

        val permDesc = TextView(this).apply {
            text = "Proqramın sönmədən işləməsi üçün icazələri növbə ilə verin:"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setPadding(0, 8, 0, 24)
        }

        statusPerm1 = TextView(this).apply { textSize = 15f; setPadding(0, 8, 0, 8) }
        statusPerm2 = TextView(this).apply { textSize = 15f; setPadding(0, 8, 0, 8) }
        statusPerm3 = TextView(this).apply { textSize = 15f; setPadding(0, 8, 0, 24) }

        actionPermButton = Button(this).apply {
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(24, 24, 24, 24)
        }

        permCard.addView(permTitle)
        permCard.addView(permDesc)
        permCard.addView(statusPerm1)
        permCard.addView(statusPerm2)
        permCard.addView(statusPerm3)
        permCard.addView(actionPermButton)

        // --- BÖLMƏ 2: DÜYMƏ TƏNZİMLƏMƏLƏRİ ---
        settingsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(40, 40, 40, 40)
            elevation = 8f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 40
            layoutParams = params
        }

        val settingsTitle = TextView(this).apply {
            text = "2. Düymə Atamaları (2 dəfə basdıqda)"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1A1A"))
            setPadding(0, 0, 0, 24)
        }
        settingsContainer.addView(settingsTitle)

        // Səs Azaltma seçimi
        spinnerVolDown = createButtonRow(
            container = settingsContainer,
            label = "🔉 Səs Azaltma düyməsinə 2 dəfə basanda:",
            prefKey = "ACTION_VOL_DOWN",
            defaultCode = "FLASHLIGHT", // İlkin olaraq Fənər seçili gəlir
            prefs = prefs
        )

        // Səs Artırma seçimi
        spinnerVolUp = createButtonRow(
            container = settingsContainer,
            label = "🔊 Səs Artırma düyməsinə 2 dəfə basanda:",
            prefKey = "ACTION_VOL_UP",
            defaultCode = "NONE",
            prefs = prefs
        )

        // Güc (Açma/Qapatma) düyməsi seçimi
        spinnerPower = createButtonRow(
            container = settingsContainer,
            label = "⚡ Açma/Qapatma (Güc) düyməsinə 2 dəfə basanda:",
            prefKey = "ACTION_POWER",
            defaultCode = "GOOGLE_PAY", // İlkin olaraq Google Pay seçili gəlir
            prefs = prefs
        )

        val infoNote = TextView(this).apply {
            text = "💡 Qeyd: Seçimi dəyişdiyiniz anda avtomatik yadda saxlanılır və dərhal işləyir."
            textSize = 13f
            setTextColor(Color.parseColor("#2E7D32"))
            setPadding(0, 24, 0, 0)
        }
        settingsContainer.addView(infoNote)

        rootLayout.addView(permCard)
        rootLayout.addView(settingsContainer)
        scrollView.addView(rootLayout)
        setContentView(scrollView)
    }

    // Hər dəfə istifadəçi ayarlar ekranından proqrama qayıdanda icazələri yenidən yoxlayır
    override fun onResume() {
        super.onResume()
        checkPermissionsStepByStep()
    }

    private fun checkPermissionsStepByStep() {
        val hasBatteryPerm = isIgnoringBatteryOptimizations()
        val hasOverlayPerm = Settings.canDrawOverlays(this)
        val hasAccessibilityPerm = isAccessibilityServiceEnabled()

        // Status mətnlərini yenilə
        statusPerm1.text = if (hasBatteryPerm) "✅ Addım 1: Arxa plan (Batareya) icazəsi verildi"
                           else "⏳ Addım 1: Arxa plan (Batareya) icazəsi gözlənilir..."
        statusPerm1.setTextColor(if (hasBatteryPerm) Color.parseColor("#2E7D32") else Color.parseColor("#D32F2F"))

        statusPerm2.text = if (hasOverlayPerm) "✅ Addım 2: Ekran üzərində açılma icazəsi verildi"
                           else "⏳ Addım 2: Ekran üzərində açılma icazəsi gözlənilir..."
        statusPerm2.setTextColor(if (hasOverlayPerm) Color.parseColor("#2E7D32") else Color.parseColor("#D32F2F"))

        statusPerm3.text = if (hasAccessibilityPerm) "✅ Addım 3: Əlçatanlıq (Accessibility) aktivdir"
                           else "⏳ Addım 3: Əlçatanlıq xidməti gözlənilir..."
        statusPerm3.setTextColor(if (hasAccessibilityPerm) Color.parseColor("#2E7D32") else Color.parseColor("#D32F2F"))

        // Hansı icazə çatışmırsa, növbə ilə yalnız onu istə
        when {
            !hasBatteryPerm -> {
                actionPermButton.text = "1-ci İcazəni Ver (Batareya Limiti)"
                actionPermButton.setBackgroundColor(Color.parseColor("#1976D2"))
                actionPermButton.isEnabled = true
                actionPermButton.setOnClickListener {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
                setSettingsEnabled(false)
            }
            !hasOverlayPerm -> {
                actionPermButton.text = "2-ci İcazəni Ver (Ekran Üzərində Göstərmə)"
                actionPermButton.setBackgroundColor(Color.parseColor("#EF6C00"))
                actionPermButton.isEnabled = true
                actionPermButton.setOnClickListener {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
                setSettingsEnabled(false)
            }
            !hasAccessibilityPerm -> {
                actionPermButton.text = "3-cü İcazəni Ver (Əlçatanlıq Xidmətini Qoş)"
                actionPermButton.setBackgroundColor(Color.parseColor("#6A1B9A"))
                actionPermButton.isEnabled = true
                actionPermButton.setOnClickListener {
                    Toast.makeText(
                        this,
                        "Siyahıdan 'Tecno Düymə İdarəedici' tapıb aktiv edin",
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                setSettingsEnabled(false)
            }
            else -> {
                // Bütün icazələr tamamlandı!
                actionPermButton.text = "✅ Sistem Aktivdir və İşləyir"
                actionPermButton.setBackgroundColor(Color.parseColor("#2E7D32"))
                actionPermButton.isEnabled = false
                setSettingsEnabled(true)
            }
        }
    }

    private fun setSettingsEnabled(enabled: Boolean) {
        settingsContainer.alpha = if (enabled) 1.0f else 0.4f
        spinnerVolDown.isEnabled = enabled
        spinnerVolUp.isEnabled = enabled
        spinnerPower.isEnabled = enabled
    }

    private fun createButtonRow(
        container: LinearLayout,
        label: String,
        prefKey: String,
        defaultCode: String,
        prefs: android.content.SharedPreferences
    ): Spinner {
        val textView = TextView(this).apply {
            text = label
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#333333"))
            setPadding(0, 20, 0, 8)
        }

        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                actionLabels
            )
        }

        // Əvvəlcədən yadda saxlanmış seçimi tap və göstər
        val savedCode = prefs.getString(prefKey, defaultCode) ?: defaultCode
        val initialIndex = actionCodes.indexOf(savedCode).coerceAtLeast(0)
        spinner.setSelection(initialIndex)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putString(prefKey, actionCodes[position]).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        container.addView(textView)
        container.addView(spinner)
        return spinner
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "$packageName/${ButtonRemapService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(expectedComponentName)
    }
}