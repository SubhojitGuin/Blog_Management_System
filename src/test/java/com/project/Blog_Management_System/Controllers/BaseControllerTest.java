package com.project.Blog_Management_System.Controllers;

import com.project.Blog_Management_System.BaseTest;
import com.project.Blog_Management_System.Security.WebSecurityConfig;
import org.springframework.context.annotation.Import;

@Import(WebSecurityConfig.class)
public abstract class BaseControllerTest extends BaseTest {
}
