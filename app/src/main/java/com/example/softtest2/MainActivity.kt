package com.example.softtest2

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.softtest2.databinding.ActivityMainBinding
import com.danbamitale.epmslib.entities.clearPinKey
import com.example.softtest2.app.AppUtils.getSampleUserData
import com.google.gson.Gson
import com.netpluspay.contactless.sdk.utils.ContactlessReaderResult
import com.netpluspay.nibssclient.models.UserData
import com.netpluspay.nibssclient.service.NetposPaymentClient
import com.pixplicity.easyprefs.library.Prefs
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import com.example.softtest2.app.AppUtils.CONFIG_DATA
import com.example.softtest2.app.AppUtils.KEY_HOLDER
import com.example.softtest2.app.AppUtils.getSavedKeyHolder
import com.example.softtest2.models.CardResult
import com.netpluspay.contactless.sdk.start.ContactlessSdk

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val gson: Gson = Gson()
    private var userData: UserData = getSampleUserData(this)
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    var netposPaymentClient: NetposPaymentClient = NetposPaymentClient

    private val makePaymentResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            if (result.resultCode == ContactlessReaderResult.RESULT_OK) {
                data?.let { i ->
                    val cardReadData = i.getStringExtra("data")!!
                    val cardResult = gson.fromJson(cardReadData, CardResult::class.java)
//                    makeCardPayment(cardResult, amountToPay.toLong())
                }
            }
            if (result.resultCode == ContactlessReaderResult.RESULT_ERROR) {
                data?.let { i ->
                    val error = i.getStringExtra("data")
                    error?.let {
//                        _result?.error("CARD_ERROR", "Card read error", it)
//                        Timber.d("ERROR_TAG===>%s", it)
//                        resultViewerTextView.text = it
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setListeners()
        configureTerminal()
    }

    private fun setListeners() {
        binding.proceed.setOnClickListener {
            doCardTransaction("200")
        }
    }

    private fun configureTerminal() {
        compositeDisposable.add(
            netposPaymentClient.init(this, Gson().toJson(userData))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ data ->
                    data?.let { response ->
                        val keyHolder = response.first
                        val configData = response.second
                        val pinKey = keyHolder?.clearPinKey
                        if (pinKey != null) {
                            Prefs.putString(KEY_HOLDER, gson.toJson(keyHolder))
                            Prefs.putString(CONFIG_DATA, gson.toJson(configData))
                            // Return success to Flutter
//                            _result?.success("Terminal configured successfully")
                        }
                    }
                }, { error ->
                    // Return error to Flutter
//                    _result?.error("CONFIG_FAILED", "Terminal configuration failed", error.localizedMessage)
//                    Timber.d("%s%s", ERROR_TAG, error.localizedMessage)
                })
        )
    }

    private fun doCardTransaction(amount: String) {
        try {
            launchContactless(makePaymentResultLauncher, amount.toDouble())
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    private fun launchContactless(
        launcher: ActivityResultLauncher<Intent>,
        amountToPay: Double,
        cashBackAmount: Double = 0.0,
    ) {
        val savedKeyHolder = getSavedKeyHolder()

        savedKeyHolder?.run {
            ContactlessSdk.readContactlessCard(
                this@MainActivity,
                launcher,
                this.clearPinKey, // "86CBCDE3B0A22354853E04521686863D" // pinKey
                amountToPay, // amount
                cashBackAmount, // cashbackAmount(optional)
            )
        } ?: run {
//            _result?.error("NO_CONFIG", "Terminal not configured", "Terminal not configured")
//            Timber.d("%s%s", ERROR_TAG, "Terminal not configured")
            configureTerminal()
        }
    }

}