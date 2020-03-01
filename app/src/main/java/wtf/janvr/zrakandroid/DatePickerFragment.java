package wtf.janvr.zrakandroid;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    static final public int STOP_DATE_PICKER = 1;
    static final public int START_DATE_PICKER = 0;
    private int mTag;

    public DatePickerFragment(int tag) {
        mTag = tag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        month++;
        String date_s = getString(R.string.date_string, day, month, year);
        switch (mTag) {
            case START_DATE_PICKER:
                TextView tv_start = getActivity().findViewById(R.id.device_start_date_text);
                tv_start.setText(date_s);
                break;
            case STOP_DATE_PICKER:
                TextView tv_stop = getActivity().findViewById(R.id.device_stop_date_text);
                tv_stop.setText(date_s);
                break;
        }
        return;
    }
}
