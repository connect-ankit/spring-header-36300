package com.inn.accept.headers;


import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.HttpMediaTypeNotAcceptableException;

@RestController
public class TestController {

    @RequestMapping(value = "/v1/error", method = {RequestMethod.GET})
    public String test() {

        //Since this returns a String and has no produces attribute, Spring defaults to text/plain or text/html
        //400 Bad request. Without produces, client gets 406
        return "406";
    }

    @RequestMapping(value = "/v2/error", method = {RequestMethod.GET})
    public void test2() {
     //This method returns nothing (void) and has no produces attribute
    // Result: 200 OK (Empty Body). Since there is no content to negotiate, Spring doesn't find a reason to throw a 406 error.
     // 2026-02-16T22:15:48.228+05:30 DEBUG 94061 --- [headers] [qtp704387627-33] m.m.a.RequestResponseBodyMethodProcessor : Ignoring error response content (if any). org.springframework.web.HttpMediaTypeNotAcceptableException: Could not parse 'Accept' header [application/json,application/type1,application/type2,application/type3,application/type4,application/type5,application/type6,application/type7,application/type8,application/type9,application/type10,application/type11,application/type12,application/type13,application/type14,application/type15,application/type16,application/type17,application/type18,application/type19,application/type20,application/type21,application/type22,application/type23,application/type24,application/type25,application/type26,application/type27,application/type28,application/type29,application/type30,application/type31,application/type32,application/type33,application/type34,application/type35,application/type36,application/type37,application/type38,application/type39,application/type40,application/type41,application/type42,application/type43,application/type44,application/type45,application/type46,application/type47,application/type48,application/type49,application/type50,application/type51,application/type52,application/type53,application/type54,application/type55,application/type56,application/type57,application/type58,application/type59,application/type60,application/type61,application/type62,application/type63,application/type64,application/type65,application/type66,application/type67,application/type68,application/type69]: Invalid mime type "[application/json, application/type1, application/type2, application/type3, application/type4, application/type5, application/type6, application/type7, application/type8, application/type9, application/type10, application/type11, application/type12, application/type13, application/type14, application/type15, application/type16, application/type17, application/type18, application/type19, application/type20, application/type21, application/type22, application/type23, application/type24, application/type25, application/type26, application/type27, application/type28, application/type29, application/type30, application/type31, application/type32, application/type33, application/type34, application/type35, application/type36, application/type37, application/type38, application/type39, application/type40, application/type41, application/type42, application/type43, application/type44, application/type45, application/type46, application/type47, application/type48, application/type49, application/type50, application/type51, application/type52, application/type53, application/type54, application/type55, application/type56, application/type57, application/type58, application/type59, application/type60, application/type61, application/type62, application/type63, application/type64, application/type65, application/type66, application/type67, application/type68, application/type69]": Too many elements
        System.out.println("test2");

    }


    @RequestMapping(value = "/v3/error", method = {RequestMethod.GET},produces = MediaType.APPLICATION_JSON_VALUE)
    public void test3() {
        System.out.println("test3");
    }
}
