package com.sqrtf.megumin


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper
import com.sqrtf.common.StringUtil
import com.sqrtf.common.activity.BaseFragment
import com.sqrtf.common.api.ApiClient
import com.sqrtf.common.api.ListResponse
import com.sqrtf.common.model.Bangumi
import com.sqrtf.megumin.homefragment.HomeData
import com.sqrtf.megumin.homefragment.HomeLineAdapter
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import java.text.SimpleDateFormat
import java.util.*


class HomeFragment : BaseFragment() {

    private val recyclerView by lazy { findViewById(R.id.recycler_view) as RecyclerView }
    private val swipeRefresh by lazy { findViewById(R.id.swipe_refresh) as SwipeRefreshLayout }
    private val homeDataAdapter by lazy { HomeDataAdapter(this) }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh.setColorSchemeResources(R.color.meguminRed)
        homeDataAdapter.attachTo(recyclerView)

        swipeRefresh.setOnRefreshListener {
            loadData()
        }

        loadData()
    }

    private fun loadData() {
        swipeRefresh.isRefreshing = true
        Observable.zip(withLifecycle(ApiClient.getInstance().getMyBangumi()),
                withLifecycle(ApiClient.getInstance().getAllBangumi()),
                BiFunction { t1: ListResponse<Bangumi>, t2: ListResponse<Bangumi> -> Pair(t1.getData(), t2.getData()) })
                .subscribe({
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    homeDataAdapter.list.clear()
                    val set = it.first.toHashSet()
//                    set.addAll(it.second)
                    val todayUpdate = set
                            .filter {
                                val week = (System.currentTimeMillis() - format.parse(it.air_date).time) / 604800000
                                return@filter week <= it.eps + 1
                            }
                            .sortedBy { format.parse(it.air_date) }
                            .reversed()

                    if (todayUpdate.isNotEmpty()) {
                        homeDataAdapter.list.add(HomeData(getString(R.string.releasing)))
                        homeDataAdapter.list.add(HomeData(todayUpdate
                                .filter { it.unwatched_count >= 1 }))
                    }

                    if (it.second.isNotEmpty()) {
                        homeDataAdapter.list.add(HomeData(getString(R.string.title_bangumi)))
                        homeDataAdapter.list.addAll(it.second.map { HomeData(it) })
                    }

                    homeDataAdapter.list.add(HomeData())
                    homeDataAdapter.notifyDataSetChanged()
                    swipeRefresh.isRefreshing = false
                }, {
                    swipeRefresh.isRefreshing = false
                    toastErrors().accept(it)
                })
    }

    private class HomeDataAdapter(val parent: Fragment) {
        val spanCount = parent.resources.getInteger(R.integer.home_screen_span_count)
        val list = arrayListOf<HomeData>()
        val adapter = HomeAdapter()
        val lm = LinearLayoutManager(parent.context)

        fun attachTo(recyclerView: RecyclerView) {
            recyclerView.layoutManager = lm
            recyclerView.adapter = adapter
        }

        fun notifyDataSetChanged() {
            adapter.notifyDataSetChanged()
        }

        class HomeLineHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var recyclerView: RecyclerView = itemView.findViewById(R.id.recyclerView) as RecyclerView
        }

        private class WideCardHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image = view.findViewById(R.id.imageView) as ImageView
            val title = view.findViewById(R.id.title) as TextView
            val subtitle = view.findViewById(R.id.subtitle) as TextView
            val info = view.findViewById(R.id.info) as TextView
            val state = view.findViewById(R.id.state) as TextView
        }

        private class TitleHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text = view.findViewById(R.id.textView) as TextView
        }

        private class TailHolder(view: View) : RecyclerView.ViewHolder(view) {
            init {
                view.setOnClickListener { view.context.startActivity(AllBangumiActivity.intent(view.context)) }
            }
        }

        private inner class HomeAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder?, position: Int) {

                val bangumi = list[position].bangumi
                when (viewHolder) {
                    is HomeLineHolder -> {
                        viewHolder.recyclerView.layoutManager = LinearLayoutManager(viewHolder
                                .recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
                        GravitySnapHelper(Gravity.START).attachToRecyclerView(viewHolder.recyclerView)
                        viewHolder.recyclerView.adapter = HomeLineAdapter(list[position], object : HomeLineAdapter.OnClickListener {
                            override fun onClick(b: Bangumi) {
                                parent.startActivity(DetailActivity.intent(parent.context, b))
                            }

                        })
                    }
                    is WideCardHolder -> {
                        if (bangumi == null) {
                            return
                        }

                        viewHolder.title.text = StringUtil.mainTitle(bangumi)
                        viewHolder.subtitle.text = StringUtil.subTitle(bangumi)
                        viewHolder.info.text = viewHolder.info.resources.getString(R.string.update_info)
                                ?.format(bangumi.air_date, bangumi.eps, bangumi.air_weekday.let { StringUtil.dayOfWeek(it) })

                        if (bangumi.favorite_status > 0) {
                            val array = viewHolder.state.resources.getStringArray(R.array.array_favorite)
                            if (array.size > bangumi.favorite_status) {
                                viewHolder.state.text = array[bangumi.favorite_status]
                            }
                        } else {
                            viewHolder.state.text = ""
                        }

                        Glide.with(parent)
                                .load(bangumi.image)
                                .into(viewHolder.image)

                        viewHolder.itemView.setOnClickListener {
                            parent.startActivity(bangumi.let { it1 -> DetailActivity.intent(parent.context, it1) })
                        }
                    }
                    is TitleHolder -> {
                        viewHolder.text.text = list[position].string
                    }
                }
            }

            override fun onCreateViewHolder(p0: ViewGroup?, p1: Int): RecyclerView.ViewHolder {
                return when (p1) {
                    HomeData.TYPE.TITLE.value -> TitleHolder(LayoutInflater.from(p0!!.context).inflate(R.layout.include_home_title, p0, false))
                    HomeData.TYPE.WIDE.value -> WideCardHolder(LayoutInflater.from(p0!!.context).inflate(R.layout.include_bangumi_wide, p0, false))
                    HomeData.TYPE.CONTAINER.value -> HomeLineHolder(LayoutInflater.from(p0!!.context).inflate(R.layout.include_home_line_container, p0, false))
                    HomeData.TYPE.TAIL.value -> TailHolder(LayoutInflater.from(p0!!.context).inflate(R.layout.include_home_tail, p0, false))
                    else -> throw RuntimeException("unknown type")
                }
            }

            override fun getItemCount(): Int {
                return list.size
            }

            override fun getItemViewType(position: Int): Int {
                return list[position].type.value
            }
        }

    }
}
