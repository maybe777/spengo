package com.voskhod.spnego.conf;

        import com.voskhod.spnego.service.AppAuthenticationSuccessHandler;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.ComponentScan;
        import org.springframework.context.annotation.Configuration;
        import org.springframework.core.io.FileSystemResource;
        import org.springframework.security.authentication.AuthenticationManager;
        import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
        import org.springframework.security.config.annotation.web.builders.HttpSecurity;
        import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
        import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
        import org.springframework.security.core.authority.AuthorityUtils;
        import org.springframework.security.core.userdetails.UserDetailsService;
        import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
        import org.springframework.security.kerberos.authentication.KerberosAuthenticationProvider;
        import org.springframework.security.kerberos.authentication.KerberosServiceAuthenticationProvider;
        import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosClient;
        import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosTicketValidator;
        import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;
        import org.springframework.security.kerberos.web.authentication.SpnegoEntryPoint;
        import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
        import org.springframework.security.core.userdetails.User;


@Configuration
@ComponentScan("com.voskhod.spnego")
@EnableWebSecurity
public class KerberosSecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${krb5.service-principal}")
    private String servicePrincipal;

    @Value("${krb5.keytab-location}")
    private String keytabLocation;

    @Value("${include.basic-auth}")
    private boolean includeBasicAuth;

    private final AppAuthenticationSuccessHandler appHandler;

    @Autowired
    public KerberosSecurityConfig(AppAuthenticationSuccessHandler appHandler) {
        this.appHandler = appHandler;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
                .csrf().disable()
                .addFilterBefore(
                        spnegoAuthenticationProcessingFilter(authenticationManagerBean()),
                        BasicAuthenticationFilter.class)
                .exceptionHandling().accessDeniedPage("/error")
                .authenticationEntryPoint(spnegoEntryPoint())
                .and()
                .authorizeRequests()
                .antMatchers("/login", "/error").permitAll()
                .antMatchers("/welcome", "/logout").hasAnyRole("USER")
                .and();

        http
                .formLogin()
                .loginPage("/login")
                .successHandler(appHandler)
                .failureUrl("/login?error")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll();

        http
                .logout()
                .permitAll()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true);

    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(kerberosAuthenticationProvider())
                .authenticationProvider(kerberosServiceAuthenticationProvider());

        if (includeBasicAuth) {
            auth.inMemoryAuthentication()
                    .withUser("user").password(passwordEncoder().encode("123")).roles("USER")
                    .and()
                    .withUser("admin").password(passwordEncoder().encode("1234")).roles("ADMIN");
        }
    }

    @Bean
    public KerberosAuthenticationProvider kerberosAuthenticationProvider() {
        KerberosAuthenticationProvider provider = new KerberosAuthenticationProvider();
        SunJaasKerberosClient client = new SunJaasKerberosClient();
        client.setDebug(true);
        UserDetailsService uds = dummyUserDetailsService();
        provider.setKerberosClient(client);
        provider.setUserDetailsService(uds);
        return provider;
    }

    @Bean
    public SpnegoEntryPoint spnegoEntryPoint() {
        return new SpnegoEntryPoint("/login");
    }

    @Bean
    public SpnegoAuthenticationProcessingFilter spnegoAuthenticationProcessingFilter(AuthenticationManager authenticationManager) {
        SpnegoAuthenticationProcessingFilter filter = new SpnegoAuthenticationProcessingFilter();
        filter.setAuthenticationManager(authenticationManager);
        return filter;
    }

    @Bean
    public KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider() {
        KerberosServiceAuthenticationProvider provider = new KerberosServiceAuthenticationProvider();
        provider.setTicketValidator(sunJaasKerberosTicketValidator());
        provider.setUserDetailsService(dummyUserDetailsService());
        return provider;
    }

    @Bean
    public SunJaasKerberosTicketValidator sunJaasKerberosTicketValidator() {
        SunJaasKerberosTicketValidator ticketValidator = new SunJaasKerberosTicketValidator();
        ticketValidator.setServicePrincipal(servicePrincipal);
        ticketValidator.setKeyTabLocation(new FileSystemResource(keytabLocation));
        ticketValidator.setDebug(true);
        return ticketValidator;
    }

    @Bean
    public UserDetailsService dummyUserDetailsService() {
        return (username) -> {
            return new User(username, "notUsed", true, true,
                    true, true, AuthorityUtils.createAuthorityList("ROLE_USER"));
        };
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(11);
    }

}
