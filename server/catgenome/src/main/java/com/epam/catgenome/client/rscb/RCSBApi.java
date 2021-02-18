package com.epam.catgenome.client.rscb;

import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto.DasalignmentDTO;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto.DatasetDTO;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RCSBApi {

    @GET("graphql")
    Call<DatasetDTO> getDataset(@Query(value = "query", encoded = true) String query);

    @GET("graphql")
    Call<DasalignmentDTO> getDasalignment(@Query(value = "query", encoded = true) String query);


}
