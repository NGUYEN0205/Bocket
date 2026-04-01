package com.example.bocket.net;

import com.example.bocket.model.Post; // Nếu bạn để Post ở package model
import java.util.List;

public class PostResponse {
    private List<Post> data;

    public List<Post> getData() {
        return data;
    }

    public void setData(List<Post> data) {
        this.data = data;
    }
}