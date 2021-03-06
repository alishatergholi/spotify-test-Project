package com.spotifyplayer.ui


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import com.spotifyplayer.R
import com.spotifyplayer.adapter.SearchPagedAdapter
import com.spotifyplayer.databinding.ActivityMainBinding
import com.spotifyplayer.db.SpotifyDb
import com.spotifyplayer.enums.Status
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : BaseActivity() {

    var binding: ActivityMainBinding? = null
    var viewModel: MainActivityViewModel? = null

    companion object {
        fun intentFor(context: Context, isTestMode: Boolean): Intent {
            val intent = Intent(context, ListWithoutPaging::class.java)
            intent.putExtra(isTestModeKey, isTestMode)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        viewModel = ViewModelProviders
            .of(
                this, MainActivityViewModel.Factory(
                    database = SpotifyDb.getInstance(this@MainActivity, false),
                    tokenRequest = restRequest,
                    isTestMode = isTestMode,
                    executer = appExecuter
                )
            ).get(MainActivityViewModel::class.java)
        setSupportActionBar(toolbar)
        subscribeToModel(viewModel!!)
        updateUiCallBack()
        updateListbySearchText()
    }

    fun subscribeToModel(viewModel: MainActivityViewModel) {
        val adapter = SearchPagedAdapter()
        recyclerView.adapter = adapter
        viewModel.networkState.observe(this, Observer {
            viewModel.showProgress(it)
            when (it.status) {
                Status.SUCCESS -> {
                    adapter.setNetworkState(it)
                }
                Status.RUNNING -> {
                    hideKeyboard()
                }
                else -> {
                    binding!!.buttonRetry.text = it.msg
                }
            }
        })
        viewModel.data.observe(this, Observer {
            adapter.submitList(it)
        })
        binding!!.buttonRetry.setOnClickListener {
            viewModel.retry()
        }
    }

    fun updateUiCallBack() {
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                updateListbySearchText()
                true
            } else {
                false
            }
        }
        searchEditText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                updateListbySearchText()
                true
            } else {
                false
            }
        }
    }

    fun updateListbySearchText() {
        searchEditText.text!!
            .trim().toString()
            .let {
                viewModel!!.searchResultShow(it)
            }
    }
}
