package com.jackcholt.reveal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.text.SpannableStringBuilder;
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
    private boolean BOOLshowFullScreen;
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
            BOOLshowFullScreen = mSharedPref.getBoolean("show_fullscreen", false);
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

            if (BOOLshowFullScreen) {
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
            } else {
                ArrayAdapter<Category> categoryAdapter = new ArrayAdapter<Category>(this, R.layout.browser_list_item,
                        R.id.book_name, mListCategories);

                setListAdapter(categoryAdapter);
            }
        } finally {
            setProgressBarIndeterminateVisibility(false);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        try {
            super.onListItemClick(l, v, position, id);

            Object selected = (Object) l.getItemAtPosition(position);

            if (selected instanceof Title) {
                if (mBusy) {
                    Toast.makeText(this, R.string.ebook_download_busy, Toast.LENGTH_LONG).show();
                } else {
                    downloadTitle((Title) selected);
                }
            } else {
                Category category = (Category) selected;

                mBreadCrumb.push(new URL(mDownloadServer + "?c=" + category.getId()));

                updateScreen();
            }
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
            switch (item.getItemId()) {
            case SEARCH_ID:
                final EditText input = new EditText(this);
                final TitleBrowser caller = this;

                AlertDialog.Builder searchDialog = new AlertDialog.Builder(this);
                {
                    searchDialog.setTitle("Search");
                    searchDialog.setView(input);
                }

                searchDialog.setPositiveButton(android.R.string.search_go, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SpannableStringBuilder value = new SpannableStringBuilder(input.getText());

                        try {
                            mBreadCrumb.push(new URL(mDownloadServer + "?s="
                                    + URLEncoder.encode(value.toString(), "UTF-8")));
                        } catch (MalformedURLException e) {
                            Util.unexpectedError(caller, e);
                        } catch (UnsupportedEncodingException e) {
                            Util.unexpectedError(caller, e);
                        }

                        updateScreen();
                    }
                });

                searchDialog.show();

                return true;
            }
        } catch (RuntimeException rte) {
            Util.unexpectedError(this, rte);
        } catch (Error e) {
            Util.unexpectedError(this, e);
        }

        return super.onMenuItemSelected(featureId, item);
    }

    private void downloadTitle(final Title title) {
        URL downloadUrl = null;

        try {
            downloadUrl = new URL(title.getUrl());
        } catch (MalformedURLException e) {
            Toast.makeText(this, R.string.ebook_download_failed_url, Toast.LENGTH_SHORT).show();
            return;
        }

        final String downloadUrlString = downloadUrl.toExternalForm();
        final String fileLocationString = title.getFileName();

        if (fileLocationString.contains("SH Images.zip")) {
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
                if (succeeded) {
                    if (message.endsWith("%")) {
                        int percent = Integer.parseInt(message.substring(0, message.length() - 1));
                        progressNotification.update(100, percent);
                    } else {
                        progressNotification.hide();
                        main.refreshNotify(message);
                        main.scheduleRefreshBookList();
                    }
                } else {
                    progressNotification.hide();
                    Util.sendNotification(TitleBrowser.this, message, android.R.drawable.stat_sys_warning,
                            getResources().getString(R.string.app_name), mNotifMgr, mNotifId++, Main.class);
                }
            }
        };
        SafeRunnable action = new SafeRunnable() {
            @Override
            public void protectedRun() {
                YbkService.requestDownloadBook(TitleBrowser.this, downloadUrlString, fileLocationString, callback);
                Toast.makeText(TitleBrowser.this, R.string.ebook_download_started, Toast.LENGTH_SHORT).show();
                progressNotification.show();
            }
        };

        // String message = getResources().getString(id, formatArgs)
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
     * XML parsing handler. This controls the sax parsing and insertion of
     * information into the title list. All data is coming from our own
     * organization on the server side.
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
            {
                category.setStartElementListener(new StartElementListener() {
                    @Override
                    public void start(Attributes attributes) {
                        mCurrentCategory.setId(attributes.getValue(CATEGORY_ID), 0);
                    }
                });
                category.setEndTextElementListener(new EndTextElementListener() {
                    @Override
                    public void end(String body) {
                        mCurrentCategory.setName(body);
                        mListCategories.add(mCurrentCategory.copy());
                        mCurrentCategory.clear();
                    }
                });
            }

            Element book = root.getChild(BOOK_TAG);
            {
                book.setEndElementListener(new EndElementListener() {
                    @Override
                    public void end() {
                        mListTitles.add(mCurrentTitle.copy());
                        mCurrentTitle.clear();
                    }
                });
                book.getChild(TITLE_ID_TAG).setEndTextElementListener(new EndTextElementListener() {
                    @Override
                    public void end(String body) {
                        mCurrentTitle.setId(body, 0);
                    }
                });
                book.getChild(TITLE_NAME_TAG).setEndTextElementListener(new EndTextElementListener() {
                    @Override
                    public void end(String body) {
                        mCurrentTitle.setName(body);
                    }
                });
                book.getChild(TITLE_SIZE_TAG).setEndTextElementListener(new EndTextElementListener() {
                    @Override
                    public void end(String body) {
                        mCurrentTitle.setFileSize(body, 0);
                    }
                });
                book.getChild(TITLE_URL_TAG).setEndTextElementListener(new EndTextElementListener() {
                    @Override
                    public void end(String body) {
                        mCurrentTitle.setUrl(body);
                    }
                });
                book.getChild(TITLE_DESCRIPTION_TAG).setEndTextElementListener(new EndTextElementListener() {
                    @Override
                    public void end(String body) {
                        mCurrentTitle.setDescription(body);
                    }
                });
                book.getChild(TITLE_CREATED_TAG).setEndTextElementListener(new EndTextElementListener() {
                    @Override
                    public void end(String body) {
                        mCurrentTitle.setCreated(body, new Date());
                    }
                });
                book.getChild(TITLE_FILENAME_TAG).setEndTextElementListener(new EndTextElementListener() {
                    @Override
                    public void end(String body) {
                        mCurrentTitle.setFileName(body);
                    }
                });
                book.getChild(TITLE_FORMAT_TAG).setEndTextElementListener(new EndTextElementListener() {
                    @Override
                    public void end(String body) {
                        mCurrentTitle.setFileFormat(body);
                    }
                });
            }

            try {
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(POPULATE_TIMEOUT);
                Xml.parse(connection.getInputStream(), Xml.Encoding.UTF_8, root.getContentHandler());
            } catch (Exception e) {
                Toast.makeText(TitleBrowser.this, R.string.ebook_list_failed, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}