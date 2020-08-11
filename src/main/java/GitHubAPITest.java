import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;

public class GitHubAPITest {

    private static final String authorization = "Bearer your_PAT";
    private static final String baseUrl = "https://api.github.com/repos/your_username/your_repo";

    public static void main(final String[] args) throws IOException, InterruptedException {
        executeExample();
    }

    private static void executeExample() throws IOException, InterruptedException {
        final String masterSHA = getMasterBranchSHA();
        createBranch(masterSHA);
        createFile();
        final String pullRequestResponse = createPullRequest();
        final String pullNumber = getPullNumber(pullRequestResponse);
        mergePullRequest(pullNumber);
        deleteBranch();
    }

    private static String get(final String path) throws IOException, InterruptedException {
        var builder = HttpRequest.newBuilder().uri(URI.create(baseUrl + path));
        builder.setHeader("Authorization", authorization);
        builder = builder.GET();
        final HttpRequest request = builder.build();

        final HttpResponse<String> response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
        return response.body();
    }

    private static String delete(final String path) throws IOException, InterruptedException {
        var builder = HttpRequest.newBuilder().uri(URI.create(baseUrl + path));
        builder.setHeader("Authorization", authorization);
        builder = builder.DELETE();
        final HttpRequest request = builder.build();

        final HttpResponse<String> response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
        return response.body();
    }

    private static String post(final String path, final String body) throws IOException, InterruptedException {
        var builder = HttpRequest.newBuilder().uri(URI.create(baseUrl + path));
        builder = builder.setHeader("Authorization", authorization);
        builder = builder.POST(BodyPublishers.ofString(body));
        final HttpRequest request = builder.build();

        final HttpResponse<String> response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
        return response.body();
    }

    private static String put(final String path, final String body) throws IOException, InterruptedException {
        var builder = HttpRequest.newBuilder().uri(URI.create(baseUrl + path));
        builder = builder.setHeader("Authorization", authorization);
        builder = builder.PUT(BodyPublishers.ofString(body));
        final HttpRequest request = builder.build();

        final HttpResponse<String> response = HttpClient.newHttpClient().send(request, BodyHandlers.ofString());
        return response.body();
    }

    private static String getResourceFile(final String filename) throws IOException {
        final var fileStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
        return new String(Objects.requireNonNull(fileStream).readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String getMasterBranchSHA() throws IOException, InterruptedException {
        final String body = get("/git/refs/heads");

        final JsonArray jsonArray = new JsonParser().parse(body).getAsJsonArray();
        final JsonObject jsonObject = jsonArray.get(0).getAsJsonObject();
        final JsonObject object = jsonObject.get("object").getAsJsonObject();

        return object.get("sha").getAsString();
    }

    private static String createBranch(final String sha) throws IOException, InterruptedException {
        final Map<String, String> CREATE_BRANCH_MAP = new HashMap<>();

        CREATE_BRANCH_MAP.put("ref", "refs/heads/new-branch");
        CREATE_BRANCH_MAP.put("sha", sha);

        final var objectMapper = new ObjectMapper();
        final String requestBody = objectMapper.writeValueAsString(CREATE_BRANCH_MAP);

        return post("/git/refs", requestBody);
    }

    private static String createFile() throws IOException, InterruptedException {
        final String fileToAdd = getResourceFile("new_file.txt");
        final byte[] byteArray = Base64.encodeBase64(fileToAdd.getBytes());
        final String encodedString = new String(byteArray);

        final Map<String, String> CREATE_MAP = new HashMap<>();

        CREATE_MAP.put("message", "New file added");
        CREATE_MAP.put("content", encodedString);
        CREATE_MAP.put("branch", "new-branch");

        final var objectMapper = new ObjectMapper();
        final String requestBody = objectMapper.writeValueAsString(CREATE_MAP);

        return put("/contents/new_file.txt", requestBody);
    }

    private static String createPullRequest() throws IOException, InterruptedException {
        final Map<String, String> CREATE_PULL_REQUEST_MAP = new HashMap<>();

        CREATE_PULL_REQUEST_MAP.put("title", "test-pull-request");
        CREATE_PULL_REQUEST_MAP.put("head", "new-branch");
        CREATE_PULL_REQUEST_MAP.put("base", "master");

        final var objectMapper = new ObjectMapper();
        final String requestBody = objectMapper.writeValueAsString(CREATE_PULL_REQUEST_MAP);

        return post("/pulls", requestBody);
    }

    private static String getPullNumber(final String pullRequestResponse) {
        final JsonObject jsonObject = new JsonParser().parse(pullRequestResponse).getAsJsonObject();
        return jsonObject.get("number").getAsString();
    }

    private static String mergePullRequest(final String pullNumber)
            throws IOException, InterruptedException {
        final Map<String, String> MERGE_MAP = new HashMap<>();

        MERGE_MAP.put("commit_message", "Merging pull request");

        final var objectMapper = new ObjectMapper();
        final String requestBody = objectMapper.writeValueAsString(MERGE_MAP);

        final String url = String.format("/pulls/%s/merge", pullNumber);

        return put(url, requestBody);
    }

    private static String deleteBranch() throws IOException, InterruptedException {
        return delete("/git/refs/heads/new-branch");
    }
}

