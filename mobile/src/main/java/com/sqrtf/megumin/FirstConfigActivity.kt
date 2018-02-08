package com.sqrtf.megumin

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.AppCompatSpinner
import android.text.TextUtils
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import com.sqrtf.common.activity.BaseActivity
import com.sqrtf.common.api.ApiClient
import com.sqrtf.common.api.LoginRequest
import com.sqrtf.common.cache.MeguminPreferences
import io.reactivex.functions.Consumer
import okhttp3.HttpUrl

class FirstConfigActivity : BaseActivity() {

    private val spinner by lazy { findViewById(R.id.spinner) as AppCompatSpinner }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_config)

        val sp = ArrayAdapter.createFromResource(this,
                R.array.array_link_type, R.layout.spinner_item)
        sp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = sp

        val textServer = findViewById(R.id.server) as EditText
        val textUser = findViewById(R.id.user) as EditText
        val textPw = findViewById(R.id.pw) as EditText

        textServer.setText(MeguminPreferences.getServer(), TextView.BufferType.EDITABLE)

        findViewById(R.id.floatingActionButton).setOnClickListener {
            val host = StringBuilder()
            val domain = textServer.text.toString()

            if (domain.isEmpty()) {
                showToast("Please enter domain")
                return@setOnClickListener
            }

            val isHttps = spinner.selectedItemPosition == 1
            host.append(if (isHttps) "https://" else "http://")
            host.append(domain)

            if (!domain.endsWith("/"))
                host.append("/")

            showToast("connecting...")

            ApiClient.init(this, host.toString())
            ApiClient.getInstance().login(LoginRequest(textUser.text.toString(), textPw.text.toString(), true))
                    .withLifecycle()
                    .subscribe(Consumer {
                        MeguminPreferences.setServer(host.toString())
                        MeguminPreferences.setUsername(textUser.text.toString())
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }, toastErrors())
        }
    }
}
