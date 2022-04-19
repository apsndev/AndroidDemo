package ru.project.demo.view.catalog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_categories.*
import kotlinx.android.synthetic.main.layout_empty_view.*
import kotlinx.android.synthetic.main.layout_retry_view.*
import ru.project.demo.R
import ru.project.demo.data.model.catalog.Category
import ru.project.demo.util.obtainViewModel
import ru.project.demo.view.base.BaseFragment

class CategoriesFragment : BaseFragment(), CategoriesAdapter.OnCategoryClickListener {

    private val adapter = CategoriesAdapter(this, this)

    private lateinit var viewModel: CategoriesViewModel

    private var callbacks: Callbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            callbacks = context as Callbacks
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implements ${Callbacks::class.java.name}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_categories, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = obtainViewModel(CategoriesViewModel::class.java)

        observeLiveData()
        initView()

        if (isScreenVisible) {
            viewModel.loadCategories()
        }
    }

    override fun onResume() {
        super.onResume()
        sendScreenView(getString(R.string.screen_categories))
    }

    override fun onDestroyView() {
        categoriesView.adapter = null
        super.onDestroyView()
    }

    override fun onDetach() {
        callbacks = null
        super.onDetach()
    }

    override fun onBecomeVisible() {
        viewModel.loadCategories()
        sendScreenView(getString(R.string.screen_categories))
    }

    override fun onCategoryClick(category: Category) {
        callbacks?.onCategorySelected(category)
    }

    private fun observeLiveData() {
        viewModel.showAuthErrorEvent.observe(viewLifecycleOwner, Observer {
            showAuthError()
        })
        viewModel.showLoadingEvent.observe(viewLifecycleOwner, Observer {
            setLoadingView(loadingView, it)
        })
        viewModel.showNoConnectionEvent.observe(viewLifecycleOwner, Observer {
            showErrorView(getString(R.string.error_no_connection))
        })
        viewModel.categoriesLiveData.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer

            if (it.isEmpty()) {
                showEmptyView()
            } else {
                hideEmptyView()
                adapter.submitList(it)
            }
        })
        viewModel.categoriesErrorEvent.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer

            showErrorView(it.getErrorString())
        })
    }

    private fun initView() {
        createOptionsMenu()
        categoriesView.adapter = adapter
        categoriesView.visibility = View.GONE
        loadingView.visibility = View.GONE
        emptyLayout.visibility = View.GONE
        retryLayout.visibility = View.GONE
        retryButton.setOnClickListener {
            viewModel.loadCategories()
        }
        emptyView.text = getString(R.string.categories_is_empty)
    }

    private fun createOptionsMenu() {
        toolbar.menu?.clear()
        toolbar.inflateMenu(R.menu.menu_categories)
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.action_search -> {
                    callbacks?.onSearchClick(null)
                    return@setOnMenuItemClickListener true
                }
            }
            return@setOnMenuItemClickListener false
        }
    }

    private fun showEmptyView() {
        categoriesView.visibility = View.GONE
        retryLayout.visibility = View.GONE
        if (emptyLayout.visibility != View.VISIBLE) {
            crossfade(appearView = emptyLayout)
        }
    }

    private fun hideEmptyView() {
        if (categoriesView.visibility != View.VISIBLE) {
            crossfade(appearView = categoriesView)
        }
        emptyLayout.visibility = View.GONE
        retryLayout.visibility = View.GONE
    }

    private fun showErrorView(error: String) {
        errorTextView.text = error
        categoriesView.visibility = View.GONE
        emptyLayout.visibility = View.GONE
        if (retryLayout.visibility != View.VISIBLE) {
            crossfade(appearView = retryLayout)
        }
    }

    interface Callbacks {
        fun onSearchClick(categoryId: String?)

        fun onCategorySelected(category: Category)
    }

    companion object {
        const val TAG = "CategoriesFragment"

        fun newInstance() = CategoriesFragment()
    }
}