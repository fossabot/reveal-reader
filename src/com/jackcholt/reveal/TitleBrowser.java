package com.jackcholt.reveal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
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

import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
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
    private int mNotifId = 0;
    private static final String TAG = "Reveal TitleBrowser";
    private Stack<URL> mBreadCrumb = new Stack<URL>();
    private List<Title> mListTitles;
    private List<Category> mListCategories;
    private SharedPreferences mSharedPref;
    private boolean mBusy = false;
    private boolean BOOLshowFullScreen;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            Util.startFlurrySession(this);
            FlurryAgent.onEvent("TitleBrowser");

            Map<String, String> flurryMap = new HashMap<String, String>();
            flurryMap.put("eBook Downloaded", "eBookname");

            mNotifMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

            mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            BOOLshowFullScreen = mSharedPref.getBoolean("show_fullscreen", false);

            if (BOOLshowFullScreen) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                requestWindowFeature(Window.FEATURE_NO_TITLE);
            }

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
    protected void onStart() {
        try {
            Util.startFlurrySession(this);
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
            FlurryAgent.onEndSession(this);
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
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        try {
            super.onListItemClick(l, v, position, id);

            Object selected = (Object) l.getItemAtPosition(position);

            if (selected instanceof Title) {
                if (mBusy) {
                    Toast.makeText(this, R.string.ebook_download_busy, Toast.LENGTH_LONG).show();
                    FlurryAgent.onError("TitleBrowser", "Download Busy", "WARNING");
                } else {
                    downloadTitle((Title) selected);
                }
            } else {
                Category category = (Category) selected;

                mBreadCrumb.push(new URL(mDownloadServer + "?c=" + category.id));

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

    private void downloadTitle(final Title title) {
        URL downloadUrl = null;
        StringBuilder information = new StringBuilder();

        try {
            downloadUrl = new URL(title.url);
        } catch (MalformedURLException e) {
            Toast.makeText(this, R.string.ebook_download_failed_url, Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.name != null) {
            information.append(title.name + "\n\n");
        }

        if (title.fileSize > 0) {
            information.append("Size: " + title.fileSize + " KB\n");
        }
        if (title.created != null) {
            information.append("Created: " + title.created + "\n");
        }
        if (title.description != null) {
            information.append("Description: " + title.description + "\n");
        }

        final String downloadUrlString = downloadUrl.toExternalForm();
        final String fileLocationString = title.fileName;
        
        if (fileLocationString.contains("SH Images.zip")) {
            HiddenEBook.create(this);
        }
        
        final ProgressNotification progressNotification = new ProgressNotification(this, mNotifId++, R.drawable.ebooksmall,
                MessageFormat.format(getResources().getString(R.string.downloading), title.name));
        final Completion callback = new Completion() {
            // @Override
            public void completed(boolean succeeded, String message) {
                Main main = Main.getMainApplication();
                if (main != null) {
                    if (succeeded) {
                        if (message.endsWith("%")) {
                            int percent = Integer.parseInt(message.substring(0, message.length()-1));
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
            }
        };
        // Create a map and add the name of the downloaded eBook to it
        Map<String, String> flurryMap = new HashMap<String, String>();
        flurryMap.put("eBook Downloaded", title.name);
        FlurryAgent.onEvent("TitleBrowser", flurryMap);

        SafeRunnable action = new SafeRunnable() {
            @Override
            public void protectedRun() {
                YbkService.requestDownloadBook(TitleBrowser.this, downloadUrlString, fileLocationString, callback);
                Toast.makeText(TitleBrowser.this, R.string.ebook_download_started, Toast.LENGTH_SHORT).show();
                progressNotification.show();
            }
        };

        // String message = getResources().getString(id, formatArgs)
        ConfirmActionDialog.confirmedAction(this, R.string.title_browser_name, information.toString(),
                R.string.download, action);
    }

    public void populate() {
        InputSource source;

        Log.d(TAG, "Fetching titles from web");

        mListTitles = new ArrayList<Title>();
        mListCategories = new ArrayList<Category>();

        try {
            URLConnection connection = mBreadCrumb.peek().openConnection();
            connection.setConnectTimeout(POPULATE_TIMEOUT);
            source = new InputSource(connection.getInputStream());

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();

            XMLReader reader = parser.getXMLReader();

            reader.setContentHandler(new ParserHandler());

            reader.parse(source);
        } catch (MalformedURLException e) {
            Toast.makeText(TitleBrowser.this, R.string.ebook_list_failed_url, Toast.LENGTH_LONG).show();
            finish();
        } catch (IOException e) {
            Toast.makeText(TitleBrowser.this, R.string.ebook_list_failed_io, Toast.LENGTH_LONG).show();
            finish();
        } catch (ParserConfigurationException e) {
            Toast.makeText(TitleBrowser.this, R.string.ebook_list_failed, Toast.LENGTH_LONG).show();
            finish();
        } catch (SAXException e) {
            Toast.makeText(TitleBrowser.this, R.string.ebook_list_failed, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * XML parsing handler. This controls the sax parsing and insertion of
     * information into the title list. All data is coming from our own
     * organization on the server side.
     * 
     * @author jwiggins
     * 
     */
    private class ParserHandler extends DefaultHandler {
        private static final String mBookTag = "book";
        private static final String mCategoryTag = "category";
        private static final String mTitleIdTag = "title_id";
        private static final String mTitleNameTag = "title_name";
        private static final String mTitleSizeTag = "title_size";
        private static final String mTitleUrlTag = "title_url";
        private static final String mTitleDescriptionTag = "title_description";
        private static final String mTitleCreatedTag = "title_created";
        private static final String mTitleFileNameTag = "title_filename";
        private static final String mTitleFormatTag = "title_format";

        private String mCurrentTag;
        private Title mCurrentTitle;
        private Category mCurrentCategory;

        public void startElement(String namespaceURI, String tagName, String qName, Attributes attributes)
                throws SAXException {
            String lowerTag = tagName.toLowerCase();

            if (lowerTag.equals(mBookTag)) {
                mCurrentTitle = new Title();
            } else if (lowerTag.equals(mCategoryTag)) {
                mCurrentCategory = new Category();

                mCurrentCategory.id = Integer.parseInt(attributes.getValue("id"));
            }

            mCurrentTag = lowerTag;
        }

        public void endElement(String namespaceURI, String tagName, String qName) throws SAXException {
            String lowerTag = tagName.toLowerCase();

            if (lowerTag.equals(mBookTag)) {
                mListTitles.add(mCurrentTitle);
            } else if (lowerTag.equals(mCategoryTag)) {
                mListCategories.add(mCurrentCategory);
            }

            mCurrentTag = null;
        }

        /**
         * Harvest text nodes
         */
        public void characters(char inCharacters[], int start, int length) {
            String xmlValue = new String(inCharacters, start, length).trim();

            xmlValue.replace("&amp;", "&");
            xmlValue.replace("&gt;", ">");
            xmlValue.replace("&lt;", "<");
            xmlValue.replace("&apos;", "'");

            if (mCategoryTag.equals(mCurrentTag)) {
                mCurrentCategory.name = xmlValue;
            } else if (mTitleIdTag.equals(mCurrentTag)) {
                mCurrentTitle.id = Integer.parseInt(xmlValue);
            } else if (mTitleNameTag.equals(mCurrentTag)) {
                mCurrentTitle.name = xmlValue;
            } else if (mTitleSizeTag.equals(mCurrentTag)) {
                mCurrentTitle.fileSize = Integer.parseInt(xmlValue);
            } else if (mTitleUrlTag.equals(mCurrentTag)) {
                mCurrentTitle.url = xmlValue;
            } else if (mTitleDescriptionTag.equals(mCurrentTag)) {
                mCurrentTitle.description = xmlValue;
            } else if (mTitleFileNameTag.equals(mCurrentTag)) {
                mCurrentTitle.fileName = xmlValue;
            } else if (mTitleCreatedTag.equals(mCurrentTag)) {
                // mCurrentTitle.created = xmlValue; TODO: parse date (could be
                // incomplete)
            } else if (mTitleFormatTag.equals(mCurrentTag)) {
                mCurrentTitle.fileFormat = xmlValue;
            }
        }
    }
}