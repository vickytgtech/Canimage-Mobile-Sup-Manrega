package com.skyindya.dwsupervisor2;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface APIInterface {

    @Multipart
    @POST("postSupervisorDetails1")
    Call<ResponseObj> uploadImages(@Part MultipartBody.Part NearImage, @Part MultipartBody.Part FarImage,
                              @Part("id") RequestBody id,@Part("Planid") RequestBody Planid,
                              @Part("VillageCode") RequestBody VillageCode,@Part("Remark") RequestBody Remark,
                               @Part("ExecutionDate") RequestBody ExecutionDate, @Part("UploadDate") RequestBody UploadDate,
                                   @Part("imei") RequestBody imei, @Part("Printno") RequestBody Printno);
}
