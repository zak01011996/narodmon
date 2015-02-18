package narodmon.ru.narodmonweather;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.widget.TextView;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        getActionBar().setIcon(android.R.drawable.ic_dialog_info);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        TextView description = (TextView) findViewById(R.id.about_description);
        description.setText(Html.fromHtml(getString(R.string.about_description)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
