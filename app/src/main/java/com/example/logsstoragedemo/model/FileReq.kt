package com.example.logsstoragedemo.model

import okhttp3.MultipartBody

data class FileReq(var file: MultipartBody.Part?) {
    constructor() : this(null)
}
