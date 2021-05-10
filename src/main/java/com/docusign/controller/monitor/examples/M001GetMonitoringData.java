package com.docusign.controller.monitor.examples;

import com.docusign.DSConfiguration;
import com.docusign.common.WorkArguments;
import com.docusign.core.model.DoneExample;
import com.docusign.core.model.Session;
import com.docusign.core.model.User;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Get monitoring data<br />
 * This example demonstrates how to get monitoring data from the Monitor API.
 */
@Controller
@RequestMapping("/m001")
public class M001GetMonitoringData extends AbstractMonitorController {

    private final Session session;
    private final User user;
    private static final String PROBLEMS_WITH_CONNECTION_ERROR_MESSAGE = "The connection string may be corrupt, please ensure that you are using the right URL.";

    @Autowired
    public M001GetMonitoringData(DSConfiguration config, Session session, User user) {
        super(config, "m001", "Get monitoring data");
        this.session = session;
        this.user = user;
    }

    @Override
    protected Object doWork(WorkArguments args, ModelMap model, HttpServletResponse response) throws Exception {
        String accessToken = this.user.getAccessToken();

        // Check if you are using the JWT authentication
        accessToken = ensureUsageOfJWTToken(accessToken, this.session);

        String requestPath = session.getBasePath() + apiUrl;

        JSONArray result =  getMonitoringData(requestPath, accessToken, model);

        // Process results
        DoneExample.createDefault(title)
                .withMessage("Results from the DataSet:GetStreamForDataset method:")
                .withJsonObject(result.toString())
                .addToModel(model);

        return DONE_EXAMPLE_PAGE;
    }

    protected JSONArray getMonitoringData(String requestPath, String accessToken, ModelMap model) throws Exception {
        // Declare variables
        boolean complete = false;
        String cursorValue = "";
        Integer limit = 1; // Amount of records you want to read in one request
        JSONArray result = new JSONArray();

        // Get monitoring data
        do
        {
            String cursorValueFormatted = (cursorValue.isEmpty()) ? cursorValue : String.format("=%s", cursorValue);

            // Add cursor value and amount of records to read to the request
            String requestParameters = String.format("/stream?cursor%s&limit=%d",
                    cursorValueFormatted, limit);

            URL fullRequestPath = new URL(requestPath + requestParameters);
            HttpURLConnection httpConnection = (HttpURLConnection) fullRequestPath.openConnection();
            httpConnection.setRequestMethod(HttpMethod.GET.toString());

            //  Construct API headers
            // step 2 start
            httpConnection.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            httpConnection.setRequestProperty(HttpHeaders.AUTHORIZATION, BEARER_AUTHENTICATION + accessToken);
            // step 2 end

            int responseCode = httpConnection.getResponseCode();
            if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                if (httpConnection.getResponseMessage() != PROBLEMS_WITH_CONNECTION_ERROR_MESSAGE){
                    throw new Exception(httpConnection.getResponseMessage());
                }

                DoneExample.createDefault(this.title)
                        .withMessage(PROBLEMS_WITH_CONNECTION_ERROR_MESSAGE)
                        .addToModel(model);
            }

            // step 3 start
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
            String temp;
            StringBuilder stringBuilder = new StringBuilder();
            while ((temp = bufferedReader.readLine()) != null) {
                stringBuilder.append(temp);
            }
            bufferedReader.close();



            httpConnection.disconnect();
            // Removing invalid symbols from the data
            String responseData = stringBuilder.toString().replaceAll("'", "");

            JSONObject object = new JSONObject(responseData);
            String endCursor = object.getString("endCursor");

            // If the endCursor from the response is the same as the one that you already have,
            // it means that you have reached the end of the records
            if (endCursor.equals(cursorValue))
            {
                complete = true;
            }
            else
            {
                cursorValue = endCursor;
                result.put(new JSONObject(responseData));
            }
        }
        while (!complete);
        //step 3 end

        return result;
    }
}