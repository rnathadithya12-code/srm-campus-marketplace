package com.example.studentmarketplace;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


// Main Spring Boot Application Class
@SpringBootApplication
public class MarketplaceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarketplaceApplication.class, args);
    }
}

// ========== DATA MODELS (ENTITIES) ==========

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    private String firstName;

    @NotEmpty
    private String lastName;

    @Column(unique = true, nullable = false)
    @Email
    private String email;

    @NotEmpty
    private String password;

    @NotEmpty
    private String phoneNumber;

    private LocalDateTime createdAt = LocalDateTime.now();
}

@Entity
@Table(name = "listings")
@Data
@NoArgsConstructor
@AllArgsConstructor
class Listing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    private String title;

    @NotEmpty
    private String description;

    @Enumerated(EnumType.STRING)
    @NotNull
    private ListingType listingType;

    @NotNull
    private Double price;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;
}

enum ListingType {
    SALE, RENTAL
}


// ========== DATA REPOSITORIES (DATABASE ACCESS) ==========

@Repository
interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
}

@Repository
interface ListingRepository extends JpaRepository<Listing, Long> {
}


// ========== DATA TRANSFER OBJECTS (DTOs) ==========
@Data
class RegisterDto {
    @NotEmpty
    private String firstName;
    @NotEmpty
    private String lastName;
    @NotEmpty @Email
    private String email;
    @NotEmpty @Size(min = 6)
    private String password;
    @NotEmpty
    private String phoneNumber;
}

@Data
class LoginDto {
    @NotEmpty @Email
    private String email;
    @NotEmpty
    private String password;
}

@Data
class CreateListingDto {
    @NotEmpty
    private String title;
    @NotEmpty
    private String description;
    @NotNull
    private ListingType listingType;
    @NotNull
    private Double price;
}

@Data
class ListingDto {
    private Long id;
    private String title;
    private String description;
    private ListingType listingType;
    private Double price;
    private LocalDateTime createdAt;
    private String sellerName;
    private String sellerPhoneNumber;
    // --- NEW FIELD FOR SECURITY CHECK ---
    private String sellerEmail;


    public ListingDto(Listing listing) {
        this.id = listing.getId();
        this.title = listing.getTitle();
        this.description = listing.getDescription();
        this.listingType = listing.getListingType();
        this.price = listing.getPrice();
        this.createdAt = listing.getCreatedAt();
        this.sellerName = listing.getSeller().getFirstName() + " " + listing.getSeller().getLastName();
        this.sellerPhoneNumber = listing.getSeller().getPhoneNumber();
        // --- NEW LOGIC ---
        this.sellerEmail = listing.getSeller().getEmail();
    }
}


// ========== SERVICES (BUSINESS LOGIC) ==========

@Service
class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterDto registerDto) {
        if (userRepository.findByEmailIgnoreCase(registerDto.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        User newUser = new User();
        newUser.setFirstName(registerDto.getFirstName());
        newUser.setLastName(registerDto.getLastName());
        newUser.setEmail(registerDto.getEmail());
        newUser.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        newUser.setPhoneNumber(registerDto.getPhoneNumber());
        return userRepository.save(newUser);
    }
}

@Service
class ListingService {
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public ListingService(ListingRepository listingRepository, UserRepository userRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found for email: " + email));
    }

    public List<ListingDto> getAllListings() {
        return listingRepository.findAll().stream().map(ListingDto::new).collect(Collectors.toList());
    }

    public Listing createListing(CreateListingDto createListingDto, String userEmail) {
        User seller = getUserByEmail(userEmail);
        Listing newListing = new Listing();
        newListing.setTitle(createListingDto.getTitle());
        newListing.setDescription(createListingDto.getDescription());
        newListing.setListingType(createListingDto.getListingType());
        newListing.setPrice(createListingDto.getPrice());
        newListing.setSeller(seller);
        return listingRepository.save(newListing);
    }

    // --- NEW METHOD ---
    public void deleteListing(Long listingId, String userEmail) {
        // Step B: Find the User making the request
        User currentUser = getUserByEmail(userEmail);
        // Step A: Find the Item
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        // Step C: The Security Check
        if (!Objects.equals(listing.getSeller().getId(), currentUser.getId())) {
            throw new SecurityException("User not authorized to delete this listing");
        }

        // Step D: Delete from Database
        listingRepository.delete(listing);
    }
}


// ========== CONTROLLERS (API ENDPOINTS) ==========

@RestController
@RequestMapping("/api/auth")
class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthService authService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterDto registerDto) {
        try {
            authService.register(registerDto);
            return ResponseEntity.ok("User registered successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginDto loginDto) {
        User user = userRepository.findByEmailIgnoreCase(loginDto.getEmail()).orElse(null);
        if (user != null && passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            return ResponseEntity.ok(user.getEmail());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }
}

@RestController
@RequestMapping("/api/listings")
class ListingController {
    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    public List<ListingDto> getAllListings() {
        return listingService.getAllListings();
    }

    @PostMapping
    public ResponseEntity<?> createListing(@RequestBody CreateListingDto createListingDto, @RequestHeader("X-User-Email") String userEmail) {
        try {
            Listing newListing = listingService.createListing(createListingDto, userEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ListingDto(newListing));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // --- NEW METHOD ---
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteListing(@PathVariable Long id, @RequestHeader("X-User-Email") String userEmail) {
        try {
            listingService.deleteListing(id, userEmail);
            return ResponseEntity.noContent().build(); // 204 No Content is a standard success response for DELETE
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}

// ========== SECURITY & CORS CONFIGURATION ==========

@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            User user = userRepository.findByEmailIgnoreCase(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getEmail())
                    .password(user.getPassword())
                    .roles("USER")
                    .build();
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

