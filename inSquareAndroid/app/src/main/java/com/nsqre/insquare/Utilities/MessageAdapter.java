package com.nsqre.insquare.Utilities;/* Created by umbertosonnino on 2/1/16  */

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nsqre.insquare.InSquareProfile;
import com.nsqre.insquare.R;

import java.util.ArrayList;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageHolder>
{

    private static final String TAG = "MessageAdapter";
    private ArrayList<Message> mDataset;
    private static ChatMessageClickListener myClickListener;
    private InSquareProfile profile;

    public MessageAdapter(Context c)
    {
        profile = InSquareProfile.getInstance(c);
        this.mDataset = new ArrayList<Message>();
    }

    //2: appena ho tutti i messaggi inizio a creare gli item
    @Override
    public MessageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_item_me,parent,false);

        MessageHolder msgHld = new MessageHolder(view);  //va a 3
        return msgHld;  //dopo aver creato il msgHld va su 4
    }

    //4: con la position nel dataset, si prende i messaggi, e setta il text nell'item, se gli id sono uguali cambia bubble
    @Override
    public void onBindViewHolder(MessageHolder holder, int position) {
        Message m = mDataset.get(position);
        holder.content.setText(m.getText());
//        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
//        String hourMinutes = "Sent @ " + sdf.format(mDataset.get(position).getCreatedAt());
        holder.username.setText(m.getName());
        holder.setSentMessage(m.getFrom().equals(profile.getUserId()));  //va a 5
    }

    //1: quando entro in una piazza, scarico n messaggi ed eseguo n volte questo
    public void addItem(Message msg)
    {
        mDataset.add(msg);
        notifyItemInserted(mDataset.size() - 1);
    }

    public Message getMessage(int position)
    {
        return this.mDataset.get(position);
    }

    public void removeItem(int position) {
        mDataset.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public void setOnClickListener(ChatMessageClickListener clickListener)
    {
        this.myClickListener = clickListener;
    }

    public static class MessageHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        private TextView content;
        private TextView username;
        private RelativeLayout relativeLayout;

        //3: si prende questi dati
        public MessageHolder(View itemView) {
            super(itemView);

            content = (TextView) itemView.findViewById(R.id.message_content);
            username = (TextView) itemView.findViewById(R.id.message_sender);
            relativeLayout = (RelativeLayout) itemView.findViewById(R.id.message_relative_layout);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            myClickListener.onItemClick(getAdapterPosition(), v);
        }

        //5: cambia bubble e allinea a destra(TODO)
        public void setSentMessage(boolean isSent) {
            if (isSent) {
                relativeLayout.setBackgroundResource(R.drawable.bubble_b);
            }
            else
                relativeLayout.setBackgroundResource(R.drawable.bubble_a);
        }
    }

    public interface ChatMessageClickListener {
        public void onItemClick(int position, View v);
    }
}
