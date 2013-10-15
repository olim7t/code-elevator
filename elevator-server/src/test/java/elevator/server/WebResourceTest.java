package elevator.server;

import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.URISyntaxException;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.*;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static org.fest.assertions.Assertions.assertThat;

public class WebResourceTest {

    @Rule
    public ElevatorServerRule elevatorServerRule = new ElevatorServerRule();

    @Test
    public void should_not_authorize_non_admin_request() {
        Response response = elevatorServerRule.target.path("/admin/maxNumberOfUsers").request().buildGet().invoke();
        assertThat(response.getStatus()).isEqualTo(UNAUTHORIZED.getStatusCode());
    }

    @Test
    public void should_initialize_maxNumberOfUsers_with_three() {
        Response response = elevatorServerRule.target
                .path("/admin/maxNumberOfUsers").request()
                .header(AUTHORIZATION, credentials("admin", "admin"))
                .buildGet().invoke();
        assertThat(response.readEntity(String.class)).isEqualTo("3");
    }

    @Test
    public void should_increase_maxNumberOfUsers() {
        Response response = elevatorServerRule.target
                .path("/admin/increaseMaxNumberOfUsers").request()
                .header(AUTHORIZATION, credentials("admin", "admin"))
                .buildGet().invoke();
        assertThat(response.readEntity(String.class)).isEqualTo("4");
    }

    @Test
    public void should_not_reset_with_unknow_user() {
        String password = null;
        try {
            password = elevatorServerRule.target.path("/player/register")
                    .queryParam("email", "player@provider.com")
                    .queryParam("pseudo", "player")
                    .queryParam("serverURL", "http://localhost").request()
                    .buildPost(null).invoke().readEntity(String.class);

            Response response = elevatorServerRule.target
                    .path("/player/reset")
                    .queryParam("email", "unkown@provider.com").request()
                    .header(AUTHORIZATION, credentials("player@provider.com", password))
                    .buildPost(null).invoke();

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
        } finally {
            if (password != null) {
                elevatorServerRule.target.path("/player/unregister")
                        .queryParam("email", "player@provider.com").request()
                        .header(AUTHORIZATION, credentials("player@provider.com", password))
                        .buildPost(null).invoke();
            }
        }
    }

    @Test
    public void should_reset() {
        String password = null;
        try {
            password = elevatorServerRule.target.path("/player/register")
                    .queryParam("email", "player@provider.com")
                    .queryParam("pseudo", "player")
                    .queryParam("serverURL", "http://localhost").request()
                    .buildPost(null).invoke().readEntity(String.class);

            Response response = elevatorServerRule.target
                    .path("/player/reset")
                    .queryParam("email", "player@provider.com").request()
                    .header(AUTHORIZATION, credentials("player@provider.com", password))
                    .buildPost(null).invoke();

            assertThat(response.getStatus()).isEqualTo(NO_CONTENT.getStatusCode());
        } finally {
            if (password != null) {
                elevatorServerRule.target.path("/player/unregister")
                        .queryParam("email", "player@provider.com").request()
                        .header(AUTHORIZATION, credentials("player@provider.com", password))
                        .buildPost(null).invoke();
            }
        }
    }

    @Test
    public void should_unregister() {
        String password = elevatorServerRule.target.path("/player/register")
                .queryParam("email", "player@provider.com")
                .queryParam("pseudo", "player")
                .queryParam("serverURL", "http://localhost").request()
                .buildPost(null).invoke().readEntity(String.class);

        Response response = elevatorServerRule.target
                .path("/player/unregister")
                .queryParam("email", "player@provider.com").request()
                .header(AUTHORIZATION, credentials("player@provider.com", password))
                .buildPost(null).invoke();

        assertThat(response.getStatus()).isEqualTo(NO_CONTENT.getStatusCode());
    }

    @Test
    public void should_resume() {
        String password = null;
        try {
            password = elevatorServerRule.target.path("/player/register")
                    .queryParam("email", "player@provider.com")
                    .queryParam("pseudo", "player")
                    .queryParam("serverURL", "http://localhost").request()
                    .buildPost(null).invoke().readEntity(String.class);

            elevatorServerRule.target.path("/player/pause")
                    .queryParam("email", "player@provider.com").request()
                    .header(AUTHORIZATION, credentials("player@provider.com", password))
                    .buildPost(null).invoke();

            Response response = elevatorServerRule.target.path("/player/resume")
                    .queryParam("email", "player@provider.com").request()
                    .header(AUTHORIZATION, credentials("player@provider.com", password))
                    .buildPost(null).invoke();

            assertThat(response.getStatus()).isEqualTo(NO_CONTENT.getStatusCode());
        } finally {
            if (password != null) {
                elevatorServerRule.target
                        .path("/player/unregister")
                        .queryParam("email", "player@provider.com").request()
                        .header(AUTHORIZATION, credentials("player@provider.com", password))
                        .buildPost(null).invoke();
            }
        }
    }

    @Test
    public void should_register_with_score() {
        String password = null;
        try {
            password = elevatorServerRule.target.path("/player/register-with-score")
                    .queryParam("email", "player@provider.com")
                    .queryParam("pseudo", "player")
                    .queryParam("serverURL", "http://localhost")
                    .queryParam("score", 493).request()
                    .header(AUTHORIZATION, credentials("admin", "admin"))
                    .buildPost(null).invoke().readEntity(String.class);

            final Response playerInfo = elevatorServerRule.target.path("/player/info")
                    .queryParam("email", "player@provider.com")
                    .request()
                    .header(AUTHORIZATION, credentials("player@provider.com", password))
                    .buildGet().invoke();

            assertThat(playerInfo.readEntity(String.class)).contains("\"score\":493");
        } finally {
            if (password != null) {
                elevatorServerRule.target
                        .path("/player/unregister")
                        .queryParam("email", "player@provider.com").request()
                        .header(AUTHORIZATION, credentials("player@provider.com", password))
                        .buildPost(null).invoke();
            }
        }
    }

    @Test
    public void should_dump_players() {
        String password = null;
        try {
            password = elevatorServerRule.target.path("/player/register")
                    .queryParam("email", "player@provider.com")
                    .queryParam("pseudo", "player")
                    .queryParam("serverURL", "http://localhost").request()
                    .buildPost(null).invoke().readEntity(String.class);

            final Response playerAsCSV = elevatorServerRule.target.path("/players.csv")
                    .queryParam("email", "player@provider.com")
                    .request()
                    .header(AUTHORIZATION, credentials("", "admin"))
                    .buildGet().invoke();

            assertThat(playerAsCSV.getHeaderString(CONTENT_TYPE)).isEqualTo("text/csv");
            assertThat(playerAsCSV.readEntity(String.class)).isEqualTo("\"player@provider.com\",\"player\",\"http://localhost\",0");
        } finally {
            if (password != null) {
                elevatorServerRule.target
                        .path("/player/unregister")
                        .queryParam("email", "player@provider.com").request()
                        .header(AUTHORIZATION, credentials("player@provider.com", password))
                        .buildPost(null).invoke();
            }
        }
    }

    @Test
    public void should_import_players() throws URISyntaxException {
//        FormDataMultiPart part = new FormDataMultiPart().field("file", getClass().getResourceAsStream("players.csv"), MediaType.TEXT_PLAIN_TYPE);
//
//        WebResource resource = Client.create().resource(url);
//        String response = resource.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(String.class, part);
//        assertEquals("Hello, World", response);
//
        elevatorServerRule.target.path("/players.csv").request().
                buildPost(Entity.entity("\"player@provider.com\",\"player\",\"http://localhost\",-2", "text/csv")).invoke();

        final Response leaderboard = elevatorServerRule.target.path("/leaderboard").request().buildGet().invoke();
        assertThat(leaderboard.readEntity(String.class)).isEqualTo("???");
        assertThat(Boolean.TRUE).isTrue();
    }

    private String credentials(String user, String password) {
        return "Basic " + printBase64Binary((user + ":" + password).getBytes());
    }

}
