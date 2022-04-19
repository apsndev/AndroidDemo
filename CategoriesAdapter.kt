package ru.project.demo.view.catalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.item_category.view.*
import ru.project.demo.R
import ru.project.demo.data.model.catalog.Category

class CategoriesAdapter(
        private val fragment: Fragment,
        var onCategoryClickListener: OnCategoryClickListener? = null
) : ListAdapter<Category, CategoriesAdapter.CategoryViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = getItem(position)

        if (category.image == null || category.image.isEmpty()) {
            holder.imageView.visibility = View.GONE
            holder.titleView.textSize = 18.0F
        } else {
            holder.imageView.visibility = View.VISIBLE
            loadImage(category.image, holder.imageView)
            holder.titleView.textSize = 14.0F
        }

        holder.titleView.text = category.name
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryItemLayout: ConstraintLayout = itemView.categoryItemLayout as ConstraintLayout
        val imageView: ImageView = itemView.imageView
        val titleView: TextView = itemView.nameView

        init {
            categoryItemLayout.setOnClickListener {
                onCategoryClickListener?.onCategoryClick(getItem(adapterPosition))
            }
        }
    }

    interface OnCategoryClickListener {
        fun onCategoryClick(category: Category)
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

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Category>() {
            override fun areItemsTheSame(
                item1: Category,
                item2: Category
            ): Boolean {
                return item1.id == item2.id
            }

            override fun areContentsTheSame(
                item1: Category,
                item2: Category
            ): Boolean {
                return item1 == item2
            }
        }
    }
}