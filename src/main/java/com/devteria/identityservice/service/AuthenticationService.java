package com.devteria.identityservice.service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.devteria.identityservice.dto.request.AuthenticationRequest;
import com.devteria.identityservice.dto.request.IntrospectRequest;
import com.devteria.identityservice.dto.request.LogoutRequest;
import com.devteria.identityservice.dto.request.RefreshRequest;
import com.devteria.identityservice.dto.response.AuthenticationResponse;
import com.devteria.identityservice.dto.response.IntrospectResponse;
import com.devteria.identityservice.entity.InvalidatedToken;
import com.devteria.identityservice.entity.User;
import com.devteria.identityservice.exception.AppException;
import com.devteria.identityservice.exception.ErrorCode;
import com.devteria.identityservice.repository.InvalidatedTokenRepository;
import com.devteria.identityservice.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;


    // thời gian tính từ lúc token được cấp đến khi có thể refresh lại token
    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;

        try {
            verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder().valid(isValid).build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {

        log.info("SignerKey: {}", SIGNER_KEY);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        var user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) throw new AppException(ErrorCode.UNAUTHENTICATED);

        var token = generateToken(user);

        return AuthenticationResponse.builder().token(token).authenticated(true).build();
    }

    //chỉ lưu các token đã logout vào bảng invalidated_token
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {

            //ngay cả khi token đã quá hạn exp nhưng vẫn còn trong thời gian refreshable.
            var signToken = verifyToken(request.getToken(), true);

            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken =
                    InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException exception) {
            log.info("Token already expired");
        }
    }

    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        var signedJWT = verifyToken(request.getToken(), true);

        var jit = signedJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken =
                InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

        invalidatedTokenRepository.save(invalidatedToken);

        var username = signedJWT.getJWTClaimsSet().getSubject();

        var user = userRepository.findByUsername(username).
                orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        var token = generateToken(user);

        return AuthenticationResponse.builder().token(token).authenticated(true).build();
    }

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("hald.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString()) // lưu id để có thể invalidate token sau này
                .claim("scope", buildScope(user))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        //Parse chuỗi token thành đối tượng SignedJWT để có thể trích xuất claim bên trong như exp, iat, jti
        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = (isRefresh)
                ? new Date(signedJWT
                        .getJWTClaimsSet()
                        .getIssueTime()
                        .toInstant()
                //refresh token : thời gian tính từ lúc token được cấp đến khi có thể refresh lại token
                        .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS)
                        .toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();
        //Kiểm tra chữ ký JWT có đúng với SIGNER_KEY không.
        var verified = signedJWT.verify(verifier);
        //expiryTime = 12:30:00
        //now = 11:15:00
        //expiryTime.after(now) → true :  Token vẫn còn hợp lệ
        if (!(verified && expiryTime.after(new Date()))) throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");

        if (!CollectionUtils.isEmpty(user.getRoles()))
            user.getRoles().forEach(role -> {
                //JwtAuthenticationConverter sẽ convert các scope
                // thành authorities để Spring Security có thể sử dụng
                // và tự động gắn prefix ROLE_ cho các role,
                // giờ ta chủ động gắn rồi để phân biệt giữa role và permission nên bên kia sẽ set prefix là ""
                stringJoiner.add("ROLE_" + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions()))
                    role.getPermissions().forEach(permission -> stringJoiner.add(permission.getName()));
            });

        return stringJoiner.toString();
    }
}
//Note: về cái buildScope ý, đầu tiên là stringJoiner.add(role.getName()); thì chỉ add role vào thôi, xong add cả permission
// thì trong token nó sẽ là "scope": "ADMIN USER" (nếu có 2 role là ADMIN và USER)
// nhưng nếu ta add thêm permission thì nó sẽ là "scope": "ADMIN USER CAN_VIEW CAN_EDIT"
// nên nếu sử dụng @PreAuthorize("hasRole('ADMIN')") hay @PreAuthorize("hasRole('CAN_VIEW')") đều được
// và khi in cái log(list authorities) ra ở cách 2 phân quyền thì nó sẽ in ra ở authorities của Spring Security là
// "ROLE_ADMIN", "ROLE_USER", "ROLE_CAN_VIEW", "ROLE_CAN_EDIT", Spring Security sẽ tự động thêm prefix ROLE_ vào trước mỗi role.
// Thì khi sử dụng @PreAuthorize("hasRole('ADMIN')") hay @PreAuthorize("hasRole('CAN_VIEW')") thì sẽ tìm trong authorities có prefix là ROLE_


//Note : sau khi thêm stringJoiner.add("ROLE_" + role.getName()); thì sẽ phân biệt được permission và role
// thì token sẽ có dạng là "scope": "ROLE_ADMIN ROLE_USER CAN_VIEW CAN_EDIT"
//lúc này Spring Security sẽ không tự động thêm prefix ROLE_ vào trước các role nữa
// vì ta đã chủ động thêm prefix ROLE_ vào trước các role khi build token rồi
// thì khi sử dụng @PreAuthorize("hasRole('ADMIN')") thì sẽ tìm trong authorities có prefix là ROLE_


// giả dụ scope của admin sẽ là "ROLE_ADMIN ROLE_USER CAN_VIEW CAN_EDIT"
//@PreAuthorize("hasRole('ADMIN')") sẽ tìm trong authorities có prefix là ROLE_
//@PreAuthorize("hasAuthority('CAN_VIEW')"), @PreAuthorize("hasAuthority('ROLE_ADMIN')") nó sẽ map chính xác authority


//Giả sử token có thông tin:
//issueTime: 10:00
//expirationTime (exp): 10:20
//refreshableDuration: 60 phút → thời gian làm mới tối đa: 11:00

//Khi logout lúc 10:45, truyền isRefresh = true:
//        expiryTime = issueTime + 60 phút = 11:00
//        👉 So sánh expiryTime.after(now) → 11:00 > 10:45 → ✅ Đúng
//👉 Nếu chữ ký đúng (verified == true) → vượt qua kiểm tra → không bị lỗi
//👉 Cho phép lưu jti vào DB → Token được chặn


//Nếu bạn truyền isRefresh = false:
//        expiryTime = exp = 10:20
//        👉 So sánh 10:20 > 10:45 → ❌ Sai
//👉 Dù chữ ký đúng → vẫn không vượt qua điều kiện verified && expiryTime.after(...)
//👉 Token bị ném lỗi UNAUTHENTICATED → Không thể lưu vào DB → Không thể chặn