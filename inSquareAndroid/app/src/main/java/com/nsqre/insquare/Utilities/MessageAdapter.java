package com.nsqre.insquare.Utilities;/* Created by umbertosonnino on 2/1/16  */

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nsqre.insquare.R;

import java.util.ArrayList;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageHolder>
{

    private static final String TAG = "MessageAdapter";
    private ArrayList<Message> mDataset;
    private static CustomClickListener myClickListener;

    public MessageAdapter(ArrayList<Message> dataset)
    {
        this.mDataset = dataset;
    }

    @Override
    public MessageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_item,parent,false);

        MessageHolder msgHld = new MessageHolder(view);
        return msgHld;
    }

    @Override
    public void onBindViewHolder(MessageHolder holder, int position) {
        Message m = mDataset.get(position);
        holder.content.setText(m.getContent());
//        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
//        String hourMinutes = "Sent @ " + sdf.format(mDataset.get(position).getDate());
        holder.username.setText(m.getSender());
    }

    public void addItem(Message msg)
    {
        mDataset.add(msg);
        notifyItemInserted(mDataset.size() - 1);
    }

    public Message getMessage(int position)
    {
        return this.mDataset.get(position);
    }

    public void removeItem(int position)
    {
        mDataset.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public void setOnClickListener(CustomClickListener clickListener)
    {
        this.myClickListener = clickListener;
    }

    public static class MessageHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {

        private TextView content;
        private TextView username;

        public MessageHolder(View itemView) {
            super(itemView);

            content = (TextView) itemView.findViewById(R.id.message_content);
            username = (TextView) itemView.findViewById(R.id.message_sender);

            itemView.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            myClickListener.onItemClick(getAdapterPosition(), v);
        }
    }

    public interface CustomClickListener {
        public void onItemClick(int position, View v);
    }
}
