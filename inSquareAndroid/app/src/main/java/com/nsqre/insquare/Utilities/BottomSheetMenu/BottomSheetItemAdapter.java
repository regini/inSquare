package com.nsqre.insquare.Utilities.BottomSheetMenu;/* Created by umbertosonnino on 4/4/16  */

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nsqre.insquare.R;
import com.nsqre.insquare.Square.RecyclerSquareAdapter;

import java.util.List;

public class BottomSheetItemAdapter extends RecyclerView.Adapter<BottomSheetItemAdapter.ViewHolder> {

    private List<BottomSheetItem> menuItems;
    private BottomSheetItemListener mListener;
    private RecyclerSquareAdapter listAdapter;
    private int listHolderPosition;

    public BottomSheetItemAdapter(List<BottomSheetItem> items, BottomSheetItemListener listener,
                                  RecyclerSquareAdapter adapter, int viewHolderPosition
    )
    {
        this.menuItems = items;
        this.mListener = listener;
        this.listAdapter = adapter;
        this.listHolderPosition = viewHolderPosition;
    }

    public void setListener(BottomSheetItemListener listener)
    {
        this.mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).
                inflate(R.layout.bottom_sheet_menu_item_adapter, parent, false ));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setData(menuItems.get(position));
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        ImageView bottomSheetIcon;
        TextView bottomSheetTitle;
        BottomSheetItem bottomSheetItem;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);

            bottomSheetIcon = (ImageView) itemView.findViewById(R.id.bottom_sheet_menu_image);
            bottomSheetTitle = (TextView) itemView.findViewById(R.id.bottom_sheet_menu_title);
        }

        public void setData(BottomSheetItem item)
        {
            this.bottomSheetItem = item;
            bottomSheetIcon.setImageResource(item.getDrawableRes());
            bottomSheetTitle.setText(item.getTitle());
        }

        @Override
        public void onClick(View v) {
            if(mListener != null)
            {
                mListener.onBottomMenuItemClick(this.bottomSheetItem, listAdapter, listHolderPosition);
            }
        }
    }

    public interface BottomSheetItemListener
    {
        void onBottomMenuItemClick(BottomSheetItem item, RecyclerSquareAdapter fragmentListElementAdapter, int listHolderPosition);
    }
}
