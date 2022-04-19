package ru.project.demo.view.catalog

import android.text.util.Linkify
import android.util.Log
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestOptions
import com.viewpagerindicator.CirclePageIndicator
import kotlinx.android.synthetic.main.item_loading_view.view.*
import kotlinx.android.synthetic.main.item_product.view.*
import kotlinx.android.synthetic.main.item_product_image.view.*
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import ru.project.demo.R
import ru.project.demo.data.model.catalog.Product
import ru.project.demo.data.model.catalog.ProductBanner
import ru.project.demo.util.getYoutubePreviewImgUrl
import ru.project.demo.util.printSpannableBonusCount
import ru.project.demo.util.printSpannableBonusCountRange
import ru.project.demo.view.youtube.openYouTubeActivity

class ProductsPagedListAdapter(
    private val fragment: Fragment,
    var onProductClickListener: OnProductClickListener? = null,
    var isLoadingEnabled: Boolean = false,
    var selectedImagePositions: SparseIntArray = SparseIntArray()
) : PagedListAdapter<Product, RecyclerView.ViewHolder>(DIFF_UTIL_CALLBACK) {

    var isWishListLoading = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            val view = inflater.inflate(R.layout.item_product, parent, false)
            ProductViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_loading_view, parent, false)
            LoadingViewHolder(view)
        }
    }

    @Suppress("DEPRECATION")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ProductViewHolder) {
            val product = getItem(position) ?: return

            val context = holder.buyButton.context

            holder.nameView.text = product.name

            val shortDescription = product.shortDescription
                .replace(Regex("\\\\n|\n"), "<br>")

            holder.descriptionView.text =
                    HtmlCompat.fromHtml(shortDescription, HtmlCompat.FROM_HTML_MODE_COMPACT)
            BetterLinkMovementMethod.linkify(Linkify.ALL, holder.descriptionView)

            if (product.modifications.isNotEmpty()) {
                val pm = product.modifications.minByOrNull { pm -> pm.price.toIntOrNull() ?: 0 }
                holder.codeView.text = context.getString(
                    R.string.vendor_code_prefix,
                    pm?.article ?: product.modifications[0].article
                )
            } else {
                holder.codeView.text = ""
            }

            if (product.prices.isNotEmpty()) {
                var currency = 1
                val pricesBonus = pricesToInt(product.prices)
                val pricesMark = pricesToInt(product.pricesMark)
                var prices=pricesBonus
                if (pricesMark[0]>0) {prices=pricesMark
                currency=2}
                if (prices.size > 1) {
                    val minPrice = prices.minOrNull()
                    if (minPrice != null) {
                        holder.priceView.printSpannableBonusCountRange(minPrice,currency)
                    } else {
                        holder.priceView.text = ""
                    }
                } else if (product.category_id=="4"){
                    holder.priceView.text = "от ${prices[0]}"
                } else {
                    holder.priceView.printSpannableBonusCount(prices[0],null,currency)
                }
            } else {
                holder.priceView.text = ""
            }

            val imagePagerAdapter = ImagePagerAdapter(createListOfBanners(product))
            holder.viewPager.adapter = imagePagerAdapter
            holder.circlePagerIndicator.setViewPager(holder.viewPager)

            if (imagePagerAdapter.banners.size < 2) {
                holder.circlePagerIndicator.visibility = View.GONE
            } else {
                holder.circlePagerIndicator.visibility = View.VISIBLE
            }

            val isWishList = product.isWishList
            if (isWishList) {
                holder.favoriteButton.setImageResource(R.drawable.ic_favorite_orange_24dp)
            } else {
                holder.favoriteButton.setImageResource(R.drawable.ic_favorite_border_orange_24dp)
            }
            holder.buyButton.isEnabled = true
            holder.buyButton.text = context.getText(R.string.button_buy)

            val isCanBuy = product.isCanBuy
            if (isCanBuy) {
                holder.buyButton.isEnabled = true
                holder.buyButton.text = context.getText(R.string.button_buy)
                holder.buyButton.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                holder.buyButton.isEnabled = false
                holder.buyButton.setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
                holder.buyButton.text = product.rejectReason
            }

            holder.viewPager.currentItem = selectedImagePositions[position]
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLoadingEnabled && position == itemCount - 1) 1 else 0
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (isLoadingEnabled) 1 else 0
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val viewPager: ViewPager = itemView.viewPager as ViewPager
        val circlePagerIndicator: CirclePageIndicator = itemView.circlePagerIndicator
        val favoriteButton: ImageButton = itemView.favoriteButton
        val codeView: TextView = itemView.codeView
        val nameView: TextView = itemView.nameView
        val descriptionView: TextView = itemView.descriptionView
        val priceView: TextView = itemView.priceView
        val buyButton: Button = itemView.buyButton

        init {
            viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {
                }
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {}
                override fun onPageSelected(position: Int) {
                    selectedImagePositions.put(adapterPosition, position)
                }
            })

            favoriteButton.setOnClickListener {
                if (isWishListLoading) return@setOnClickListener

                val product = getItem(adapterPosition) ?: return@setOnClickListener

                val isWishList = product.isWishList

                if (isWishList) {
                    favoriteButton.setImageResource(R.drawable.ic_favorite_border_orange_24dp)
                } else {
                    favoriteButton.setImageResource(R.drawable.ic_favorite_orange_24dp)
                }
                product.isWishList = !isWishList
                notifyItemChanged(adapterPosition)

                if (isWishList) {
                    onProductClickListener?.onDeleteFromWishList(product)
                } else {
                    onProductClickListener?.onAddToWishList(product)
                }
            }

            buyButton.setOnClickListener {
                val product = getItem(adapterPosition) ?: return@setOnClickListener
                onProductClickListener?.onBuyProduct(product)
            }
        }
    }

    inner class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val loadingView: ProgressBar = itemView.loadingView
    }

    inner class ImagePagerAdapter(var banners: List<ProductBanner> = ArrayList()) : PagerAdapter() {

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view == `object` as View
        }

        override fun getCount() = banners.size

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater = LayoutInflater.from(container.context)
            val view = inflater.inflate(R.layout.item_product_image, container, false)

            val imageView: ImageView = view.imageView
            val playButton: ImageButton = view.playButton

            val banner = banners[position]
            loadImage(banner.bannerUrl, imageView)

            if (banner.type == ProductBanner.Type.TYPE_VIDEO) {
                playButton.visibility = View.VISIBLE

                val context = playButton.context
                playButton.setOnClickListener {
                    val videoId = banner.videoId ?: return@setOnClickListener
                    val title = banner.title ?: return@setOnClickListener

                    context?.openYouTubeActivity(videoId, title)
                }
            } else {
                playButton.visibility = View.GONE
            }

            (container as ViewPager).addView(view)
            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            (container as ViewPager).removeView(`object` as View)
        }
    }

    interface OnProductClickListener {
        fun onBuyProduct(product: Product)

        fun onAddToWishList(product: Product)

        fun onDeleteFromWishList(product: Product)
    }

    private fun loadImage(url: String, imageView: ImageView) {
        if (url.isNullOrEmpty()) return
        val glideUrl = GlideUrl(url)
        Glide.with(fragment)
            .load(glideUrl)
            .apply(
                RequestOptions()
                    .error(R.color.lightGray)
                    .placeholder(R.color.lightGray)
            )
            .into(imageView)
    }

    private fun createListOfBanners(product: Product): List<ProductBanner> {
        val list = ArrayList<ProductBanner>()
        if (product.video.isNotBlank()) {
            val videoPreview = product.video.getYoutubePreviewImgUrl()
            if (videoPreview.isNotBlank()) {
                list.add(
                    ProductBanner(
                        ProductBanner.Type.TYPE_VIDEO, videoPreview,
                        product.video, product.name
                    )
                )
            }
        }

        for (img in product.images) {
            if (img.isNotBlank()) {
                list.add(ProductBanner(ProductBanner.Type.TYPE_IMAGE, img))
            }
        }

        if (list.isEmpty()) {
            list.add(ProductBanner(ProductBanner.Type.TYPE_IMAGE, ""))
        }
        return list
    }

    private fun pricesToInt(priceStrings: List<String>): List<Int> {
        val prices = ArrayList<Int>()
        try {
            for (ps in priceStrings) {
                prices.add(ps.toInt())
            }
        } catch (e: Exception) {
            Log.e(TAG, "pricesToInt error: $e")
        }
        return prices
    }

    companion object {
        val DIFF_UTIL_CALLBACK = object : DiffUtil.ItemCallback<Product>() {

            override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
                return oldItem.productId == newItem.productId
            }

            override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
                return oldItem == newItem
            }
        }

        const val TAG = "ProductsPagedAdapter"
    }
}