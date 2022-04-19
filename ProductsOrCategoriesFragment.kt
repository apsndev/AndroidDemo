package ru.project.demo.view.catalog

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import kotlinx.android.synthetic.main.fragment_products_or_categories.*
import kotlinx.android.synthetic.main.layout_empty_view.*
import kotlinx.android.synthetic.main.layout_retry_view.*
import kotlinx.android.synthetic.main.layout_tune_products.*
import ru.project.demo.R
import ru.project.demo.data.model.catalog.Catalog
import ru.project.demo.data.model.catalog.Category
import ru.project.demo.data.model.catalog.Product
import ru.project.demo.util.*
import ru.project.demo.view.base.BaseFragment
import ru.project.demo.view.dialog.BuyProductDialog
import ru.project.demo.view.dialog.BuyProductViewModel

class ProductsOrCategoriesFragment : BaseFragment(),
    CategoriesAdapter.OnCategoryClickListener,
    ProductsPagedListAdapter.OnProductClickListener {

    private var categoriesAdapter: CategoriesAdapter? = null
    private var productsPagedListAdapter: ProductsPagedListAdapter? = null
    private lateinit var viewModel: ProductsOrCategoriesViewModel
    private var buyProductViewModel: BuyProductViewModel? = null
    private var callbacks: Callbacks? = null
    private var viewType = VIEW_TYPE_PRODUCTS
    private var badgeView: TextView? = null

    private val onCollapseListener: OnCollapseListener = object : OnCollapseListener {
        override fun onCollapse(view: View) {
            tuneRootLayout.animate()
                .alpha(0.0F)
                .setDuration(250)
                .withEndAction {
                    tuneRootLayout?.visibility = View.GONE
                    tuneRootLayout?.alpha = 1.0F
                }
                .start()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            callbacks = context as Callbacks
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implements ${Callbacks::class.java.name}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_products_or_categories,
            container,
            false
        )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = obtainViewModel(ProductsOrCategoriesViewModel::class.java)
        viewModel.categoryId = arguments?.getString(BUNDLE_CATEGORY_ID, null)
        buyProductViewModel = appCompatActivity?.obtainViewModel(BuyProductViewModel::class.java)
        observeLiveData()
        initView()
        viewModel.loadInitialCatalog()
    }

    override fun onResume() {
        super.onResume()
        sendScreenView(getString(R.string.screen_products))
    }

    override fun onPause() {
        productsPagedListAdapter?.isWishListLoading = false
        super.onPause()
    }

    override fun onDestroyView() {
        tuneRootLayout.clearAnimation()
        categoriesView.adapter = null
        super.onDestroyView()
    }

    override fun onDetach() {
        callbacks = null
        super.onDetach()
    }

    override fun onBecomeVisible() {
        sendScreenView(getString(R.string.screen_products))
    }

    override fun onCategoryClick(category: Category) {
        callbacks?.onCategorySelected(category)
    }

    override fun onBuyProduct(product: Product) {
        buyProductViewModel?.product = product
        val dialog = BuyProductDialog.newInstance(coordinatorLayout.height)
        fragmentManager?.let { dialog.show(it, TAG) }
    }

    override fun onAddToWishList(product: Product) {
        viewModel.chancgeToWishList(product)
    }

    override fun onDeleteFromWishList(product: Product) {
        viewModel.chancgeToWishList(product)
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
        viewModel.showCategoriesEvent.observe(viewLifecycleOwner, Observer {
            initCategoriesView()
        })
        viewModel.showProductsEvent.observe(viewLifecycleOwner, Observer {
            initProductsView()
        })
        viewModel.categoriesLiveData.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer

            if (it.isEmpty()) {
                showEmptyView()
            } else {
                hideEmptyView()
                categoriesAdapter?.submitList(it)
            }
        })
        viewModel.categoriesErrorEvent.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer

            showErrorView(it.getErrorString())
        })
        viewModel.productsLiveData.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer

            if (it.isEmpty()) {
                showEmptyView()
            } else {
                hideEmptyView()
                setupProductsAdapter()
                productsPagedListAdapter?.submitList(it)
            }
        })

        viewModel.productsLiveData2.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer

            if (it.isEmpty()) {
                showEmptyView()
            } else {
                hideEmptyView()
                setupProductsAdapter()
                productsPagedListAdapter?.submitList(it)
            }
        })
        viewModel.productsErrorEvent.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer

            showErrorView(it.getErrorString())
        })
        viewModel.isLoadingEnabledEvent.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer

            val adapter = productsPagedListAdapter
            if (adapter != null) {
                if (it) {
                    adapter.isLoadingEnabled = it
                } else {
                    adapter.isLoadingEnabled = it
                    adapter.notifyItemRemoved(adapter.itemCount)
                }
            }
        })
        viewModel.priceRangeLiveData.observe(viewLifecycleOwner, Observer {
            setupPriceRange(
                priceRangeSlider,
                priceView1,
                priceView2,
                it,
                viewModel.currentMinPrice,
                viewModel.currentMaxPrice
            )
        })
        viewModel.addWishListSuccessEvent.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer

            Toast.makeText(
                context,
                getString(R.string.product_added_to_wishlist, it.name),
                Toast.LENGTH_SHORT
            ).show()
        })
        viewModel.deleteWishListSuccessEvent.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer

            Toast.makeText(
                context,
                getString(R.string.product_delete_from_wishlist, it.name),
                Toast.LENGTH_SHORT
            ).show()
        })
        viewModel.wishListLoadingEvent.observe(viewLifecycleOwner, Observer {
            productsPagedListAdapter?.isWishListLoading = it == true
        })
        viewModel.wishListErrorEvent.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer

            Toast.makeText(context, it.getErrorString(), Toast.LENGTH_LONG).show()
        })
        viewModel.showFilterBadgeEvent.observe(viewLifecycleOwner, Observer {
            if (it == null) return@Observer

            if (it > 0) {
                badgeView?.visibility = View.VISIBLE
                badgeView?.text = it.toString()
            } else {
                badgeView?.visibility = View.GONE
            }
        })
        viewModel.catalogLiveData.observe(viewLifecycleOwner, Observer {
            setConsumerTypeFilter(it)
        })
    }

    private fun initView() {
        toolbar.title = arguments?.getString(BUNDLE_CATEGORY_NAME, "") ?: ""
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        toolbar.setNavigationOnClickListener {
            if (tuneRootLayout?.visibility == View.VISIBLE) {
                tuneLayout?.expandOrCollapse(false, onCollapseListener)
            } else {
                activity?.onBackPressed()
            }
        }

        handleBackPressed()

        val categoryImage = arguments?.getString(BUNDLE_CATEGORY_IMAGE)
        if (categoryImage.isNullOrEmpty()) {
            typeRadioGroup.visibility = View.VISIBLE
        } else {
            typeRadioGroup.visibility = View.GONE
        }

        loadingView.visibility = View.GONE
        retryLayout.visibility = View.GONE

        resetButton.setOnClickListener {
            allRadioButton.isChecked = true
            ascendingRadioButton.isChecked = true
            allConsumersRadioButton.isChecked = true

            val min = viewModel.priceRangeLiveData.value?.first
            val max = viewModel.priceRangeLiveData.value?.second
            if (min != null && max != null) {
                priceRangeSlider?.getThumb(1)?.value = max
                priceRangeSlider?.getThumb(0)?.value = min
                priceView1?.text = min.formatNumber()
                priceView2?.text = max.formatNumber()
            }

            tuneLayout.expandOrCollapse(false, onCollapseListener)
            viewModel.resetFilter()
        }

        retryButton.setOnClickListener {
            viewModel.reloadInitialCatalog()
        }

        val colorStateListRipple = ColorStateList(
            arrayOf(intArrayOf(0)),
            intArrayOf(ContextCompat.getColor(context as Context, R.color.lightGray))
        )

        val rippleDrawable = priceRangeSlider.background as RippleDrawable
        rippleDrawable.setColor(colorStateListRipple)
        priceRangeSlider.background = rippleDrawable

        emptyView.text = getString(R.string.products_is_empty)

        priceRangeSlider?.setOnThumbValueChangeListener { _, _, thumbIndex, value ->
            calculatePriceView(priceRangeSlider, priceView1, priceView2, thumbIndex, value)
        }

        initPriceViews(
            priceRangeSlider,
            priceView1,
            priceView2
        )
    }

    private fun handleBackPressed() {
        val view = this.view
        if (view != null) {
            view.isFocusableInTouchMode = true
            view.requestFocus()
            view.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                    if (tuneRootLayout.visibility == View.VISIBLE) {
                        tuneLayout.expandOrCollapse(false, onCollapseListener)
                        return@setOnKeyListener true
                    }
                }
                return@setOnKeyListener false
            }
        }
    }

    private fun createOptionsMenu() {
        toolbar.menu?.clear()
        if (viewType == VIEW_TYPE_PRODUCTS) {
            toolbar.inflateMenu(R.menu.menu_products)

            val menuItem = toolbar.menu?.findItem(R.id.action_tune)
            val actionView = menuItem?.actionView
            actionView?.setOnClickListener {
                if (tuneRootLayout.visibility == View.VISIBLE) {
                    tuneLayout.expandOrCollapse(false, onCollapseListener)
                } else {
                    tuneRootLayout.visibility = View.VISIBLE
                    tuneLayout.expandOrCollapse(true)
                }
            }
            badgeView = actionView?.findViewById(R.id.badgeView)
            badgeView?.visibility = View.GONE
        } else {
            toolbar.inflateMenu(R.menu.menu_categories)
        }
        toolbar.setOnMenuItemClickListener {
            when (it?.itemId) {
                R.id.action_search -> {
                    callbacks?.onSearchClick(viewModel.categoryId)
                    return@setOnMenuItemClickListener true
                }
            }
            return@setOnMenuItemClickListener false
        }
    }

    private fun initCategoriesView() {
        viewType = VIEW_TYPE_CATEGORIES
        productsView.visibility = View.GONE
        categoriesAdapter = CategoriesAdapter(this, this)
        categoriesView.adapter = categoriesAdapter
    }

    private fun initProductsView() {
        viewType = VIEW_TYPE_PRODUCTS
        categoriesView.visibility = View.GONE
        createOptionsMenu()
        tuneLayout.setOnClickListener {
        }
        tuneRootLayout.setOnClickListener {
            tuneLayout.expandOrCollapse(false, onCollapseListener)
        }

        applyButton.setOnClickListener { setFilters() }
        (productsView.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
        setupProductsAdapter()
    }

    private fun setupProductsAdapter() {
        val isLoadingEnabled = productsPagedListAdapter?.isLoadingEnabled ?: false
        productsPagedListAdapter = ProductsPagedListAdapter(
            this, this,
            selectedImagePositions = viewModel.selectedImagePositions
        )
        productsPagedListAdapter?.isLoadingEnabled = isLoadingEnabled
        productsView.adapter = productsPagedListAdapter
    }

    private fun showEmptyView() {
        if (viewType == VIEW_TYPE_PRODUCTS) {
            productsView.visibility = View.GONE
        } else {
            categoriesView.visibility = View.GONE
        }
        retryLayout.visibility = View.GONE
        if (emptyLayout.visibility != View.VISIBLE) {
            crossfade(appearView = emptyLayout)
        }
    }

    private fun hideEmptyView() {
        retryLayout.visibility = View.GONE
        emptyLayout.visibility = View.GONE
        if (viewType == VIEW_TYPE_PRODUCTS) {
            if (productsView.visibility != View.VISIBLE) {
                crossfade(appearView = productsView)
            }
        } else {
            if (categoriesView.visibility != View.VISIBLE) {
                crossfade(appearView = categoriesView)
            }
        }
    }

    private fun showErrorView(error: String) {
        if (viewType == VIEW_TYPE_PRODUCTS) {
            productsView.visibility = View.GONE

            val adapter = productsPagedListAdapter
            if (adapter != null && adapter.itemCount > 0) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                adapter.isLoadingEnabled = false
                adapter.notifyItemRemoved(adapter.itemCount)
            } else {
                emptyLayout.visibility = View.GONE
                errorTextView.text = error
                if (retryLayout.visibility != View.VISIBLE) {
                    crossfade(appearView = retryLayout)
                }
            }
        } else {
            categoriesView.visibility = View.GONE

            emptyLayout.visibility = View.GONE
            errorTextView.text = error
            if (retryLayout.visibility != View.VISIBLE) {
                crossfade(appearView = retryLayout)
            }
        }
    }

    private fun setFilters() {
        if (viewType != VIEW_TYPE_PRODUCTS) return
        val priceTo = priceRangeSlider.getThumb(1).value
        val priceFrom = priceRangeSlider.getThumb(0).value
        val type = when (typeRadioGroup.checkedRadioButtonId) {
            R.id.allRadioButton -> Product.GiftType.TYPE_ALL
            R.id.digitalRadioButton -> Product.GiftType.TYPE_DIGITAL
            R.id.realRadioButton -> Product.GiftType.TYPE_PHYSICAL
            else -> Product.GiftType.TYPE_ALL
        }

        val sort = when (sortRadioGroup.checkedRadioButtonId) {
            R.id.ascendingRadioButton -> Catalog.Sort.SORT_BY_ASCENDING
            R.id.descendingRadioButton -> Catalog.Sort.SORT_BY_DESCENDING
            else -> Catalog.Sort.SORT_NO_SORT
        }

        val consumerType = when (consumerTypeRadioGroup.checkedRadioButtonId) {
            R.id.allConsumersRadioButton -> Product.ConsumerType.TYPE_ALL
            R.id.forYourselfRadioButton -> Product.ConsumerType.TYPE_FOR_YOURSELF
            R.id.forHerRadioButton -> Product.ConsumerType.TYPE_FOR_HER
            R.id.forChildrenRadioButton -> Product.ConsumerType.TYPE_FOR_CHILDREN
            R.id.forHomeRadioButton -> Product.ConsumerType.TYPE_FOR_HOME
            R.id.forBusinessRadioButton -> Product.ConsumerType.TYPE_FOR_BUSINESS
            else -> Product.ConsumerType.TYPE_ALL
        }
        tuneLayout.expandOrCollapse(false, onCollapseListener)
        viewModel.setFilters(priceFrom, priceTo, type, sort, consumerType)
    }

    interface Callbacks {
        fun onSearchClick(categoryId: String?)

        fun onCategorySelected(category: Category)
    }

    private fun setConsumerTypeFilter(catalog: Catalog?) {
        if (catalog == null) return

        val availableConsumerTypes = catalog.filter.availableConsumerTypes
        if (availableConsumerTypes != null) {
            if (availableConsumerTypes.isEmpty()) {
                consumerTypeView?.visibility = View.GONE
                consumerTypeRadioGroup?.visibility = View.GONE
            } else {
                consumerTypeView?.visibility = View.VISIBLE
                consumerTypeRadioGroup?.visibility = View.VISIBLE

                for (i in 1..5) { //времено
                    val radioButton = consumerTypeRadioGroup?.getChildAt(i)
                    if (availableConsumerTypes.contains(i)) {
                        radioButton?.visibility = View.VISIBLE
                    } else {
                        radioButton?.visibility = View.GONE
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "ProductsOrCategoriesFr"
        private const val BUNDLE_CATEGORY_ID = "BUNDLE_CATEGORY_ID"
        private const val BUNDLE_CATEGORY_NAME = "BUNDLE_CATEGORY_NAME"
        private const val BUNDLE_CATEGORY_IMAGE = "BUNDLE_CATEGORY_IMAGE"
        private const val VIEW_TYPE_CATEGORIES = 0
        private const val VIEW_TYPE_PRODUCTS = 1

        fun newInstance(
            categoryId: String, categoryName: String,
            categoryImage: String?
        ): ProductsOrCategoriesFragment {
            val fragment = ProductsOrCategoriesFragment()
            val args = Bundle()
            args.putString(BUNDLE_CATEGORY_ID, categoryId)
            args.putString(BUNDLE_CATEGORY_NAME, categoryName)
            args.putString(BUNDLE_CATEGORY_IMAGE, categoryImage)
            fragment.arguments = args
            return fragment
        }
    }
}