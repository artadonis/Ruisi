package xyz.yluo.ruisiapp.activity;

import android.animation.Animator;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.yluo.ruisiapp.App;
import xyz.yluo.ruisiapp.R;
import xyz.yluo.ruisiapp.adapter.SimpleListAdapter;
import xyz.yluo.ruisiapp.data.ListType;
import xyz.yluo.ruisiapp.data.SimpleListData;
import xyz.yluo.ruisiapp.httpUtil.HttpUtil;
import xyz.yluo.ruisiapp.httpUtil.ResponseHandler;
import xyz.yluo.ruisiapp.listener.LoadMoreListener;
import xyz.yluo.ruisiapp.utils.GetId;
import xyz.yluo.ruisiapp.utils.GetNumber;
import xyz.yluo.ruisiapp.utils.ImeUtil;

/**
 * Created by free2 on 16-4-6.
 * 搜索activity
 * 搜索换页目的是获得searchid这个参数，然后加上page 参数即可
 * http://bbs.rs.xidian.me/search.php?mod=forum&amp;searchid=1268&amp;orderby=lastpost&amp;ascdesc=desc&amp;searchsubmit=yes&amp;page=20&amp;mobile=2
 */
public class ActivitySearch extends BaseActivity implements LoadMoreListener.OnLoadMoreListener {


    private int totalPage = 1;
    private int currentPage = 1;
    private int searchid = 0;
    private boolean isEnableLoadMore = false;

    private RecyclerView recycler_view;
    private EditText search_input;
    private CoordinatorLayout main_window;
    private SwipeRefreshLayout refreshLayout;
    private SimpleListAdapter adapter;
    private List<SimpleListData> datas = new ArrayList<>();
    private CardView search_card;
    private Animator animator;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        ImageView btn_back = (ImageView) findViewById(R.id.btn_back);
        recycler_view = (RecyclerView) findViewById(R.id.recycler_view);
        search_input = (EditText) findViewById(R.id.search_input);
        main_window = (CoordinatorLayout) findViewById(R.id.main_window);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_view);
        search_card = (CardView) findViewById(R.id.search_card);
        refreshLayout.setColorSchemeResources(R.color.red_light, R.color.green_light, R.color.blue_light, R.color.orange_light);
        findViewById(R.id.start_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start_search_click();
            }
        });
        refreshLayout.setEnabled(false);
        search_input.setHint("请输入搜索内容！");

        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        adapter = new SimpleListAdapter(ListType.SERRCH, this, datas);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recycler_view.setLayoutManager(layoutManager);
        recycler_view.addOnScrollListener(new LoadMoreListener((LinearLayoutManager) layoutManager, this, 20));
        recycler_view.setAdapter(adapter);

        search_input.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                boolean handled = false;
                if (i == EditorInfo.IME_ACTION_SEARCH) {
                    start_search_click();
                    handled = true;
                }
                return handled;
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            search_card.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    v.removeOnLayoutChangeListener(this);
                    animator = ViewAnimationUtils.createCircularReveal(
                            search_card,
                            search_card.getWidth(),
                            0,
                            0,
                            (float) Math.hypot(search_card.getWidth(), search_card.getHeight()));

                    animator.setInterpolator(new AccelerateInterpolator());
                    animator.setDuration(450);
                    animator.start();

                    animator.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {

                        }
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            ImeUtil.show_ime(ActivitySearch.this, search_input);
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {

                        }
                        @Override
                        public void onAnimationRepeat(Animator animator) {

                        }
                    });
                }
            });
        }else{
            ImeUtil.show_ime(ActivitySearch.this, search_input);
        }
    }

    private void start_search_click() {
        if (search_input.getText().toString().isEmpty()) {
            Snackbar.make(main_window, "你还没写内容呢", Snackbar.LENGTH_SHORT).show();
            return;
        } else {
            getData(search_input.getText().toString());
        }

        ImeUtil.hide_ime(this);
        datas.clear();
        adapter.notifyDataSetChanged();
        isEnableLoadMore = true;
        adapter.setShowLoadMore(true);
        searchid = 0;
    }

    private void getData(String str) {

        refreshLayout.post(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(true);
            }
        });

        String url = "search.php?mod=forum&mobile=2";
        Map<String, String> paras = new HashMap<>();
        paras.put("formhash", App.FORMHASH);
        paras.put("searchsubmit", "yes");
        paras.put("srchtxt", str);

        HttpUtil.post(this, url, paras, new ResponseHandler() {
            @Override
            public void onSuccess(byte[] response) {
                String res = new String(response);
                if (res.contains("秒内只能进行一次搜索")) {
                    Snackbar.make(main_window, "抱歉，您在 15 秒内只能进行一次搜索", Snackbar.LENGTH_SHORT).show();
                } else {
                    new GetResultListTaskMe().execute(new String(response));
                }
            }

            @Override
            public void onFailure(Throwable e) {
                e.printStackTrace();
                Snackbar.make(main_window, "网络错误", Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onFinish() {
                super.onFinish();
                refreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshLayout.setRefreshing(false);
                    }
                }, 500);

            }
        });
    }

    private void getSomePageData(int page) {
        String url = "search.php?mod=forum&searchid=" + searchid + "&orderby=lastpost&ascdesc=desc&searchsubmit=yes&page=" + page + "&mobile=2";

        HttpUtil.get(this, url, new ResponseHandler() {
            @Override
            public void onSuccess(byte[] response) {
                new GetResultListTaskMe().execute(new String(response));
            }

            @Override
            public void onFailure(Throwable e) {
                isEnableLoadMore = true;
                e.printStackTrace();
                Snackbar.make(main_window, "网络错误(Error -2)", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onLoadMore() {
        //loadmore 被出发
        //加载更多被电击
        if (isEnableLoadMore) {
            isEnableLoadMore = false;
            int page = currentPage;
            if (currentPage < totalPage && totalPage > 1 && searchid > 0) {
                Log.i("loadmore", currentPage + "");
                page = page + 1;
                getSomePageData(page);
            }
        }
    }

    private class GetResultListTaskMe extends AsyncTask<String, Void, List<SimpleListData>> {
        @Override
        protected List<SimpleListData> doInBackground(String... params) {
            String res = params[0];
            List<SimpleListData> dataset = new ArrayList<>();
            Document doc = Jsoup.parse(res);
            Elements body = doc.select("div[class=threadlist]"); // 具有 href 属性的链接
            //获得总页数
            //获取总页数 和当前页数
            if (doc.select(".pg").text().length() > 0) {
                Elements pageinfos = doc.select(".pg");
                currentPage = GetNumber.getNumber(pageinfos.select("strong").text());
                int n = GetNumber.getNumber(pageinfos.select("span").attr("title"));
                if (n > 0 && n > totalPage) {
                    totalPage = n;
                }
                if (totalPage > 1) {
                    //searchid = pageinfos.select("a").attr("href")
                    searchid = GetId.getSearchId(pageinfos.select("a").attr("href"));
                    Log.i("search id", searchid + "");
                }
                Log.i("page info", doc.select(".pg").html());
                Log.i("page info", currentPage + " " + totalPage);
            }

            Elements links = body.select("li");
            for (Element src : links) {
                String url = src.select("a").attr("href");
                String title = src.select("a").html();
                dataset.add(new SimpleListData(title, "", url));
            }
            return dataset;
        }

        @Override
        protected void onPostExecute(List<SimpleListData> dataset) {
            if (dataset.size() == 0) {
                adapter.setShowLoadMore(false);
                isEnableLoadMore = false;
            } else {
                adapter.setShowLoadMore(true);
                isEnableLoadMore = true;

                if (currentPage >= totalPage) {
                    adapter.setShowLoadMore(false);
                    isEnableLoadMore = false;
                }
                int start = datas.size();
                datas.addAll(dataset);
                adapter.notifyItemChanged(start);
                adapter.notifyItemRangeInserted(start + 1, dataset.size());
            }
            findViewById(R.id.view_loading).setVisibility(View.GONE);
            recycler_view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshLayout.setRefreshing(false);
                }
            }, 300);
        }
    }
}
