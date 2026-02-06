package com.example.easymove.model;

/**
 * Interface defining the minimal data required to display an item in the Chats List.
 * Any object implementing this (like {@link Chat}) can be displayed by the ChatsListAdapter.
 */
public interface ChatListItem {

    /**
     * @return The unique ID of the chat (Firestore Document ID).
     */
    String getId();

    /**
     * @return The title to display (usually the name of the OTHER participant).
     */
    String getChatTitle();

    /**
     * @return The URL of the profile image to display.
     */
    String getChatImageUrl();

    /**
     * @return The text content of the last message sent in this chat.
     */
    String getLastMessageText();

    /**
     * @return The timestamp of the last activity in milliseconds. Used for sorting.
     */
    long getTimestampLong();
}