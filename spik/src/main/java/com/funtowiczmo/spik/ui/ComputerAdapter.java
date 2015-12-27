package com.funtowiczmo.spik.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.funtowiczmo.spik.R;
import com.polytech.spik.domain.Computer;

import java.util.List;

/**
 * Created by momo- on 01/11/2015.
 */
public class ComputerAdapter extends ArrayAdapter<Computer>{

    public ComputerAdapter(Context context, List<Computer> objects) {
        super(context, R.layout.computer_item, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if(convertView == null){
            final LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.computer_item, parent, false);
            convertView.setTag(new ComputerItem(convertView));
        }

        final ComputerItem tag = (ComputerItem) convertView.getTag();
        final Computer computer = getItem(position);

        tag.computerName.setText(computer.name());

        return convertView;
    }

    static class ComputerItem {
        @Bind(R.id.computer_name)
        public TextView computerName;

        public ComputerItem(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
