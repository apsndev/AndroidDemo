package ru.project.demo.view.catalog

import android.util.SparseIntArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import ru.project.demo.R
import ru.project.demo.data.model.UiError
import ru.project.demo.data.model.catalog.Catalog
import ru.project.demo.data.model.catalog.CatalogFilter
import ru.project.demo.data.model.catalog.Category
import ru.project.demo.data.model.catalog.Product
import ru.project.demo.data.repository.CatalogRepository
import ru.project.demo.util.SingleLiveEvent
import ru.project.demo.util.isNetworkConnected
import ru.project.demo.view.base.BaseViewModel

class ProductsOrCategoriesViewModel(private val catalogRepository: CatalogRepository) :
    BaseViewModel() {

    var categoryId: String? = null

    var currentMinPrice = -1
    var currentMaxPrice = -1

    var selectedImagePositions = SparseIntArray()

    private val _showCategoriesEvent = SingleLiveEvent<Unit>()
    val showCategoriesEvent: LiveData<Unit> get() = _showCategoriesEvent

    private val _showProductsEvent = SingleLiveEvent<Unit>()
    val showProductsEvent: LiveData<Unit> get() = _showProductsEvent

    private val _categoriesLiveData = MutableLiveData<List<Category>>()
    val categoriesLiveData: LiveData<List<Category>> get() = _categoriesLiveData

    private val _categoriesErrorEvent = SingleLiveEvent<UiError>()
    val categoriesErrorEvent: LiveData<UiError> get() = _categoriesErrorEvent

    private val _productsLiveData = MutableLiveData<PagedList<Product>>()
    val productsLiveData: LiveData<PagedList<Product>> get() = _productsLiveData

    private val _productsLiveData2 = MutableLiveData<PagedList<Product>>()
    val productsLiveData2: LiveData<PagedList<Product>> get() = _productsLiveData2

    private val _productsErrorEvent = SingleLiveEvent<UiError>()
    val productsErrorEvent: LiveData<UiError> get() = _productsErrorEvent

    private val _addWishListSuccessEvent = SingleLiveEvent<Product>()
    val addWishListSuccessEvent: LiveData<Product> get() = _addWishListSuccessEvent

    private val _deleteWishListSuccessEvent = SingleLiveEvent<Product>()
    val deleteWishListSuccessEvent: LiveData<Product> get() = _deleteWishListSuccessEvent

    private val _wishListLoadingEvent = SingleLiveEvent<Boolean>()
    val wishListLoadingEvent: LiveData<Boolean> get() = _wishListLoadingEvent

    private val _wishListErrorEvent = SingleLiveEvent<UiError>()
    val wishListErrorEvent: LiveData<UiError> get() = _wishListErrorEvent

    private val _isLoadingEnabledEvent = SingleLiveEvent<Boolean>()
    val isLoadingEnabledEvent: LiveData<Boolean> get() = _isLoadingEnabledEvent

    private val _priceRangeLiveData = MutableLiveData<Pair<Int, Int>>()
    val priceRangeLiveData: LiveData<Pair<Int, Int>> get() = _priceRangeLiveData

    private val _catalogLiveData = MutableLiveData<Catalog>()
    val catalogLiveData: LiveData<Catalog> get() = _catalogLiveData

    private val _showFilterBadgeEvent = MutableLiveData<Int>()
    val showFilterBadgeEvent: LiveData<Int> get() = _showFilterBadgeEvent

    private var catalogFilter: CatalogFilter? = CatalogFilter(
        null,
        null,
        null,
        Catalog.Sort.SORT_BY_ASCENDING,
        null
    )

    fun loadInitialCatalog() {
        if (categoriesLiveData.value != null) {
            _showCategoriesEvent.call()
            _showLoadingEvent.value = false
            return
        } else if (_productsLiveData.value != null) {
            _showProductsEvent.call()
            _showLoadingEvent.value = false
            return
        }

        reloadInitialCatalog()
    }

    fun setFilters(
        priceFrom: Int,
        priceTo: Int,
        type: Product.GiftType,
        sort: Catalog.Sort,
        consumerType: Product.ConsumerType
    ) {
        val newFilter = CatalogFilter(priceFrom, priceTo, type, sort, consumerType.type)

        if (newFilter != this.catalogFilter) {
            this.catalogFilter = newFilter
            loadProducts(filter = catalogFilter)
        }

        calculateFiltersCount()
    }

    private fun calculateFiltersCount() {
        val minPrice = priceRangeLiveData.value?.first
        val maxPrice = priceRangeLiveData.value?.second

        val currentMinPrice = catalogFilter?.priceFrom
        val currentMaxPrice = catalogFilter?.priceTo

        var count = 0
        if (minPrice != null && currentMinPrice != null && currentMinPrice != minPrice) {
            count++
        }
        if (maxPrice != null && currentMaxPrice != null && currentMaxPrice != maxPrice) {
            count++
        }

        val currentType = catalogFilter?.type
        if (currentType != null && currentType != Product.GiftType.TYPE_ALL) {
            count++
        }

        val currentSort = catalogFilter?.sort
        if (currentSort != null && currentSort != Catalog.Sort.SORT_BY_ASCENDING) {
            count++
        }

        val currentConsumerType = catalogFilter?.consumerType
        if (currentConsumerType != null
            && currentConsumerType != Product.ConsumerType.TYPE_ALL.type
        ) {
            count++
        }

        _showFilterBadgeEvent.value = count
    }

    fun resetFilter() {
        val resetFilter = CatalogFilter(
            null, null,
            null, Catalog.Sort.SORT_BY_ASCENDING, null
        )
        if (resetFilter != this.catalogFilter) {
            this.catalogFilter = resetFilter
            loadProducts(filter = catalogFilter)
        }

        _showFilterBadgeEvent.value = 0
    }

    private fun loadProducts(catalog: Catalog? = null, filter: CatalogFilter? = null) {
        val categoryId = this.categoryId ?: return

        val products = _productsLiveData.value
        if (products == null) {
            _showLoadingEvent.value = true
        }

        disposableManager.add(
            catalogRepository.loadProducts(
                categoryId, _isLoadingEnabledEvent,
                _productsErrorEvent, disposableManager, catalog, filter
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    selectedImagePositions.clear()
                    _productsLiveData.value = it

                    _showLoadingEvent.value = false
                }, {
                    handleProductsError(it)

                    _showLoadingEvent.value = false
                })
        )
    }

    fun reloadInitialCatalog() {
        val categoryId = this.categoryId ?: return

        if (!isNetworkConnected()) {
            _showNoConnectionEvent.call()
            return
        }
        _showLoadingEvent.value = true
        disposableManager.add(
            catalogRepository.loadProductsOrCategories(
                categoryId,
                filter = catalogFilter
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.categories != null && it.categories.isNotEmpty()) {
                        _showCategoriesEvent.call()
                        _categoriesLiveData.value = it.categories

                        _showLoadingEvent.value = false
                    } else if (it.products != null) {
                        _showProductsEvent.call()
                        _catalogLiveData.value = it
                        loadProducts(it, catalogFilter)
                    }

                    val minPrice = it.filter.minPrice
                    val maxPrice = it.filter.maxPrice
                    if (minPrice != null && maxPrice != null) {
                        catalogFilter = CatalogFilter(
                            minPrice,
                            maxPrice,
                            catalogFilter?.type,
                            catalogFilter?.sort,
                            catalogFilter?.consumerType
                        )
                        _priceRangeLiveData.value = Pair(minPrice, maxPrice)
                    }
                }, {
                    handleCatalogError(it)

                    _showLoadingEvent.value = false
                })
        )
    }

    fun chancgeToWishList(product: Product) {
        _wishListLoadingEvent.value = true
        disposableManager.add(
            catalogRepository.addToWishList(product)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    _addWishListSuccessEvent.value = product

                    _wishListLoadingEvent.value = false
                }, {
                    handleWishListError(it)

                    _wishListLoadingEvent.value = false
                })
        )
    }

    private fun handleWishListError(throwable: Throwable) {
        if (throwable is HttpException) {
            when (throwable.code()) {
                401 -> _showAuthErrorEvent.call()
                404 -> _wishListErrorEvent.value = UiError(R.string.error_catalog_not_found)
                else -> _wishListErrorEvent.value = UiError(R.string.error_add_to_wishlist)
            }
        } else {
            _wishListErrorEvent.value = UiError(R.string.error_add_to_wishlist)
        }
    }

    private fun handleCatalogError(throwable: Throwable) {
        if (throwable is HttpException) {
            when (throwable.code()) {
                401 -> _showAuthErrorEvent.call()
                404 -> _categoriesErrorEvent.value = UiError(R.string.error_catalog_not_found)
                else -> _categoriesErrorEvent.value = UiError(R.string.error_loading_categories)
            }
        } else {
            _categoriesErrorEvent.value = UiError(R.string.error_loading_categories)
        }
    }

    private fun handleProductsError(throwable: Throwable) {
        if (throwable is HttpException) {
            when (throwable.code()) {
                401 -> _showAuthErrorEvent.call()
                404 -> _productsErrorEvent.value = UiError(R.string.error_catalog_not_found)
                else -> _productsErrorEvent.value = UiError(R.string.error_loading_products)
            }
        } else {
            _productsErrorEvent.value = UiError(R.string.error_loading_products)
        }
    }

    companion object {
        const val TAG = "ProductsOrCategoriesVM"
    }
}