package vn.utc.tracuu.platform.security;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.utc.tracuu.access.UserRepository;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public AppUserDetails loadUserByUsername(String username) {
        return users.findByUsernameWithDepartments(username)
                .map(AppUserDetails::from)
                // Thông điệp này KHÔNG bao giờ ra tới người dùng — lối ra chung ở AuthService
                // dùng một câu duy nhất, không phân biệt sai tên với sai mật khẩu.
                .orElseThrow(() -> new UsernameNotFoundException("khong co tai khoan"));
    }
}
