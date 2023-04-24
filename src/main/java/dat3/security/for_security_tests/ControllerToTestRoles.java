package dat3.security.for_security_tests;

import dat3.security.service.UserWithRolesService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@Getter
@Setter
class TestResponse {
    String UserName;
    String message;

    public TestResponse(String userName, String message) {
        UserName = userName;
        this.message = message;
    }
}
@RestController
@RequestMapping("/api/security-tests")
@Profile("test")  //This ensures that this controller is only loaded when the "test" profile is active, so it does not interfere with the real controller
public class ControllerToTestRoles {

    UserWithRolesService userWithRolesService;

    public ControllerToTestRoles(UserWithRolesService userWithRolesService) {
        this.userWithRolesService = userWithRolesService;
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/admin")
    public TestResponse getAdminUser(Principal principal) {
        return new TestResponse(userWithRolesService.getUserWithRoles(principal.getName()).getUserName(),"Admin");
    }
    @PreAuthorize("hasAuthority('USER')")
    @GetMapping("/user")
    public TestResponse getUserUser(Principal principal) {
        return new TestResponse(userWithRolesService.getUserWithRoles(principal.getName()).getUserName(),"User");
    }

    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    @GetMapping("/useradmin")
    public TestResponse getUserOrAdminUser(Principal principal) {
        return new TestResponse(userWithRolesService.getUserWithRoles(principal.getName()).getUserName(),"User and/or Amin");
    }

    @GetMapping("/authenticated")
    public TestResponse getAuthenticatedUser(Principal principal) {
        return new TestResponse(userWithRolesService.getUserWithRoles(principal.getName()).getUserName(),"Authenticated user");
    }
}