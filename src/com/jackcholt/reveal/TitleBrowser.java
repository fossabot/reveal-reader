package com.jackcholt.reveal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.jackcholt.reveal.YbkService.Completion;
import com.jackcholt.reveal.data.Category;
import com.jackcholt.reveal.data.Title;

/**
 * List activity to show categories and titles under those categories
 * 
 * @author jwiggins
 * 
 */
public class TitleBrowser extends ListActivity {

    private static String mDownloadServer = "http://revealreader.thepackhams.com/catalog/list.php";
    public static final String TITLE_LOOKUP_URL = "http://revealreader.thepackhams.com/catalog/getTitle.php?file=";
    public static final int POPULATE_TIMEOUT = 5000;
    private NotificationManager mNotifMgr;
    private int mNotifId = Integer.MIN_VALUE;
    private static final String TAG = "Reveal TitleBrowser";
    private Stack<URL> mBreadCrumb = new Stack<URL>();
    private List<Title> mListTitles = new ArrayList<Title>();
    private List<Category> mListCategories = new ArrayList<Category>();
    private SharedPreferences mSharedPref;
    private boolean mBusy = false;
    private boolean mShowFullScreen;
    public ProgressDialog mProgDialog;
    private static final int SEARCH_ID = Menu.FIRST;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            Map<String, String> flurryMap = new HashMap<String, String>();
            flurryMap.put("eBook Downloaded", "eBookname");

            mNotifMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

            mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            mShowFullScreen = mSharedPref.getBoolean("show_fullscreen", false);
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

            if (mShowFullScreen) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                requestWindowFeature(Window.FEATURE_NO_TITLE);
            }
            Util.setTheme(mSharedPref, this);
            setContentView(R.layout.browser_main);

            // setup current location in stack
            Object lastStack = getLastNonConfigurationInstance();

            if (lastStack != null) {
                for (Object url : (Stack<?>) lastStack) {
                    mBreadCrumb.push((URL) url);
                }
            } else {
                mBreadCrumb.push(new URL(mDownloadServer));
            }

            updateScreen();
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        } catch (MalformedURLException me) {
            Util.unexpectedError(this, me);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Util.setTheme(mSharedPref, this);
    }

    @Override
    protected void onStart() {
        try {
            super.onStart();
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    @Override
    protected void onStop() {
        try {
            super.onStop();
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mBreadCrumb;
    }

    protected void updateScreen() {
        try {
            setProgressBarIndeterminateVisibility(true);
            populate();

            if (mListTitles.size() > 0) {
                ArrayAdapter<Title> titleAdapter = new ArrayAdapter<Title>(this, R.layout.browser_list_item,
                        R.id.book_name, mListTitles);

                setListAdapter(titleAdapter);
                return;
            }

            ArrayAdapter<Category> categoryAdapter = new ArrayAdapter<Category>(this, R.layout.browser_list_item,
                    R.id.book_name, mListCategories);

            setListAdapter(categoryAdapter);
        } finally {
            setProgressBarIndeterminateVisibility(false);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        try {
            super.onListItemClick(l, v, position, id);

            Object selected = l.getItemAtPosition(position);
            if (selected instanceof Title) {
                if (mBusy) {
                    Toast.makeText(this, R.string.ebook_download_busy, Toast.LENGTH_LONG).show();
                    return;
                }

                downloadTitle((Title) selected);
                return;
            }

            Category category = (Category) selected;
            mBreadCrumb.push(new URL(mDownloadServer + "?c=" + category.getId()));
            updateScreen();
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        } catch (MalformedURLException e) {
            Util.unexpectedError(this, e);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        try {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (mBreadCrumb.size() > 1) {
                    mBreadCrumb.pop();

                    updateScreen();
                } else {
                    finish();
                }
                return false;
            }

            if (keyCode == KeyEvent.KEYCODE_SEARCH) {
                startSearch();
                return true;
            }
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        try {
            super.onCreateOptionsMenu(menu);
            menu.add(Menu.NONE, SEARCH_ID, Menu.NONE, R.string.title_search).setIcon(android.R.drawable.ic_menu_search);
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }

        return true;
    }

    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
        try {
            if (SEARCH_ID == item.getItemId()) {
                startSearch();
                return true;
            }
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }

        return super.onMenuItemSelected(featureId, item);
    }

    public void startSearch() {
        final EditText input = new EditText(this);
        final TitleBrowser caller = this;

        AlertDialog.Builder searchDialog = new AlertDialog.Builder(this)
                .setPositiveButton(android.R.string.search_go, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            mBreadCrumb.push(new URL(
                                    mDownloadServer
                                            + "?s="
                                            + URLEncoder.encode(new SpannableStringBuilder(input.getText()).toString(),
                                                    "UTF-8")));
                        } catch (MalformedURLException e) {
                            Util.unexpectedError(caller, e);
                        } catch (UnsupportedEncodingException e) {
                            Util.unexpectedError(caller, e);
                        }

                        updateScreen();
                    }
                }).setTitle(R.string.search_title).setView(input);

        searchDialog.show();
    }

    private void downloadTitle(final Title title) {
        URL downloadUrl = null;

        try {
            downloadUrl = new URL(title.getUrl());
        } catch (MalformedURLException e) {
            Toast.makeText(this, R.string.ebook_download_failed_url, Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.getFileName().contains("SH Images.zip")) {
            HiddenEBook.create(this);
        }

        final ProgressNotification progressNotification = new ProgressNotification(this, mNotifId++,
                R.drawable.ebooksmall, MessageFormat.format(getResources().getString(R.string.downloading),
                        title.getName()));

        final Completion callback = new Completion() {
            public void completed(boolean succeeded, String message) {
                Main main = Main.getMainApplication();
                if (null == main) {
                    return;
                }

                if (!succeeded) {
                    progressNotification.hide();
                    Util.sendNotification(TitleBrowser.this, message, android.R.drawable.stat_sys_warning,
                            getResources().getString(R.string.app_name), mNotifMgr, mNotifId++, Main.class);
                    return;
                }

                if (message.endsWith("%")) {
                    int percent = Integer.parseInt(message.substring(0, message.length() - 1));
                    progressNotification.update(100, percent);
                    return;
                }

                progressNotification.hide();
                main.refreshNotify(message);
                main.scheduleRefreshBookList();
            }
        };

        final String downloadUrlString = downloadUrl.toExternalForm();
        SafeRunnable action = new SafeRunnable() {
            @Override
            public void protectedRun() {
                YbkService.requestDownloadBook(TitleBrowser.this, downloadUrlString, title.getFileName(), callback);
                Toast.makeText(TitleBrowser.this, R.string.ebook_download_started, Toast.LENGTH_SHORT).show();
                progressNotification.show();
            }
        };

        ConfirmActionDialog.confirmedAction(this, R.string.title_browser_name, title.getSynopsis(), R.string.download,
                action);
    }

    public void populate() {
        Log.d(TAG, "Fetching titles from web");

        mListTitles.clear();
        mListCategories.clear();

        new TitleParser(mBreadCrumb.peek());
    }

    /**
     * XML parsing handler. This controls the sax parsing and insertion of information into the title list. All data is
     * coming from our own organization on the server side.
     * 
     * @author jwiggins
     * 
     */
    private class TitleParser {
        private static final String LISTING = "listing";
        private static final String BOOK_TAG = "book";
        private static final String CATEGORY_TAG = "category";
        private static final String CATEGORY_ID = "id";
        private static final String TITLE_ID_TAG = "title_id";
        private static final String TITLE_NAME_TAG = "title_name";
        private static final String TITLE_SIZE_TAG = "title_size";
        private static final String TITLE_URL_TAG = "title_url";
        private static final String TITLE_DESCRIPTION_TAG = "title_description";
        private static final String TITLE_CREATED_TAG = "title_created";
        private static final String TITLE_FILENAME_TAG = "title_filename";
        private static final String TITLE_FORMAT_TAG = "title_format";

        public TitleParser(URL url) {
            final Title mCurrentTitle = new Title();
            final Category mCurrentCategory = new Category();
            RootElement root = new RootElement(LISTING);

            Element category = root.getChild(CATEGORY_TAG);

            category.setStartElementListener(new StartElementListener() {
                public void start(Attributes attributes) {
                    mCurrentCategory.setId(attributes.getValue(CATEGORY_ID), 0);
                }
            });
            category.setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    mCurrentCategory.setName(body);
                    mListCategories.add(mCurrentCategory.copy());
                    mCurrentCategory.clear();
                }
            });

            Element book = root.getChild(BOOK_TAG);

            book.setEndElementListener(new EndElementListener() {
                public void end() {
                    mListTitles.add(mCurrentTitle.copy());
                    mCurrentTitle.clear();
                }
            });
            book.getChild(TITLE_ID_TAG).setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    mCurrentTitle.setId(body, 0);
                }
            });
            book.getChild(TITLE_NAME_TAG).setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    mCurrentTitle.setName(body);
                }
            });
            book.getChild(TITLE_SIZE_TAG).setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    mCurrentTitle.setFileSize(body, 0);
                }
            });
            book.getChild(TITLE_URL_TAG).setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    mCurrentTitle.setUrl(body);
                }
            });
            book.getChild(TITLE_DESCRIPTION_TAG).setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    mCurrentTitle.setDescription(body);
                }
            });
            book.getChild(TITLE_CREATED_TAG).setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    mCurrentTitle.setCreated(body, null);
                }
            });
            book.getChild(TITLE_FILENAME_TAG).setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    mCurrentTitle.setFileName(body);
                }
            });
            book.getChild(TITLE_FORMAT_TAG).setEndTextElementListener(new EndTextElementListener() {
                public void end(String body) {
                    mCurrentTitle.setFileFormat(body);
                }
            });

            mProgDialog = new ProgressDialog(TitleBrowser.this);
            mProgDialog.setCancelable(true);
            mProgDialog.setMessage(TitleBrowser.this.getResources().getText(R.string.list_is_loading));
            mProgDialog.show();

            new UpdateTitleList().execute(new Object[] { url, root });
        }
    }

    private class UpdateTitleList extends AsyncTask<Object, Void, String> {
        RootElement mRoot;

        @Override
        protected String doInBackground(Object... params) {
            URL url = (URL) params[0];
            mRoot = (RootElement) params[1];

            try {
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(POPULATE_TIMEOUT);
                return Util.convertStreamToString(connection.getInputStream());
            } catch (IOException ioe) {
                Log.w(TAG, "List update failed on IOException. " + ioe.getMessage() + " " + ioe.getCause());
                return "";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                if (TextUtils.isEmpty(result)) {
                    Toast.makeText(TitleBrowser.this, R.string.ebook_list_failed, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                try {
                    Xml.parse(result, mRoot.getContentHandler());
                } catch (SAXException se) {
                    Log.w(TAG, "List update failed on SAXException. " + se.getMessage() + " " + se.getCause());
                    Toast.makeText(TitleBrowser.this, R.string.ebook_list_failed, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                if (mListTitles.size() > 0) {
                    setListAdapter(new ArrayAdapter<Title>(TitleBrowser.this, R.layout.browser_list_item,
                            R.id.book_name, mListTitles));
                    return;
                }

                setListAdapter(new ArrayAdapter<Category>(TitleBrowser.this, R.layout.browser_list_item,
                        R.id.book_name, mListCategories));
            } finally {
                mProgDialog.dismiss();
                super.onPostExecute(result);
            }
        }
    }
}