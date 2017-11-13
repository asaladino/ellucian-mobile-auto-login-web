#import "AutoLoginWebViewController.h"
#import "ModuleProperty.h"
#import "CurrentUser.h"

@implementation AutoLoginWebViewController

- (void) viewDidLoad {
    [super viewDidLoad];
    self.mbProgressHUD = [MBProgressHUD showHUDAddedTo:self.view animated:YES];
}

- (void) viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    self.allowInsecure = YES;
    self.webView.hidden = YES;
    self.mbProgressHUD.hidden = NO;
    self.gotPostAttempt = NO;
}

- (void) viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self loadUser];
    [self loadLogin];
}

- (void) viewWillDisappear:(BOOL)animated {
    [self clearCache];
}

- (void) loadUser {
    CurrentUser *user = [CurrentUser sharedInstance];
    self.username = user.userauth;
    self.password = [user getPassword];
}

- (void) loadLogin {
    self.title = self.module.name;
    self.loginUrl = [self getPropertyForModule: @"loginUrl"];
    self.usernameSelector = [self getPropertyForModule: @"usernameSelector"];
    self.passwordSelector = [self getPropertyForModule: @"passwordSelector"];
    self.formSelector = [self getPropertyForModule: @"formSelector"];
    self.extraFields = [self getPropertyForModule: @"extraFields"];

    self.redirectUrl = [self getPropertyForModule: @"redirectUrl"];
    self.redirectSelector = [self getPropertyForModule: @"redirectSelector"];
    self.cssUrl = [self getPropertyForModule: @"cssUrl"];
    self.jsUrl = [self getPropertyForModule: @"jsUrl"];

    if([self.loginUrl hasPrefix:@"https://"] || self.allowInsecure) {
        NSURLRequest *request = [[NSURLRequest alloc] initWithURL: [NSURL URLWithString: self.loginUrl] cachePolicy: NSURLRequestReloadIgnoringCacheData timeoutInterval: 10000];
        [self.webView loadRequest: request];
    } else {
        self.mbProgressHUD.hidden = YES;
        [self showMessage:@"Site is not secure."];
    }
}

- (BOOL) webView:(UIWebView *)webView shouldStartLoadWithRequest:(NSURLRequest *)request navigationType:(UIWebViewNavigationType)navigationType {
    if ([[[request URL] absoluteString] hasPrefix: @"app://foundForm"] && !self.gotPostAttempt) {
        NSLog(@"url: %@", request.URL.absoluteString);
        self.gotPostAttempt = YES;
        [self handlePageRequest];
    }
    return YES;
}

- (void) webViewDidFinishLoad:(UIWebView *) webView {
    [self loadResourses];
    [self loadExternalResources];
    [self checkLogin];
    [self checkLoggedInAndRedirect];
}

- (void) handlePageRequest {
    NSString *command = [[NSString alloc] initWithFormat:@"AutoLoginWeb.loadUserIn('%@','%@');", self.username, self.password];
    [self.webView stringByEvaluatingJavaScriptFromString:command];
    [self waitForPost];
}

- (void) checkLogin {
    NSString *command = [[NSString alloc] initWithFormat:@"AutoLoginWeb.checkForFields();"];
    [self.webView stringByEvaluatingJavaScriptFromString:command];
}

- (void) checkLoggedInAndRedirect {
    if (![self.redirectUrl isEqualToString:@""] && ![self.redirectSelector isEqualToString:@""]) {
        NSString *command = [[NSString alloc] initWithFormat:@"AutoLoginWeb.checkLoggedInAndRedirect();"];
        [self.webView stringByEvaluatingJavaScriptFromString:command];

    }
}

- (void) loadResourses {
    NSString *js = [self getResourceContents:@"auto-login-web.js"];
    js = [js stringByReplacingOccurrencesOfString:@"\n" withString:@" "];
    js = [js stringByReplacingOccurrencesOfString:@"{{extraFields}}" withString:self.extraFields];
    js = [js stringByReplacingOccurrencesOfString:@"{{usernameSelector}}" withString:self.usernameSelector];
    js = [js stringByReplacingOccurrencesOfString:@"{{passwordSelector}}" withString:self.passwordSelector];
    js = [js stringByReplacingOccurrencesOfString:@"{{formSelector}}" withString:self.formSelector];
    js = [js stringByReplacingOccurrencesOfString:@"{{redirectUrl}}" withString:self.redirectUrl];
    js = [js stringByReplacingOccurrencesOfString:@"{{redirectSelector}}" withString:self.redirectSelector];
    [self.webView stringByEvaluatingJavaScriptFromString:js];
}

- (void) loadExternalResources {
    if (![self.cssUrl isEqualToString:@""]) {
        NSString *command = [[NSString alloc] initWithFormat:@"AutoLoginWeb.addCss('auto-login-css','%@');", self.cssUrl];
        [self.webView stringByEvaluatingJavaScriptFromString:command];
    }
    if (![self.jsUrl isEqualToString:@""]) {
        NSString *command = [[NSString alloc] initWithFormat:@"AutoLoginWeb.addJs('%@');", self.jsUrl];
        [self.webView stringByEvaluatingJavaScriptFromString:command];
    }
}

- (void) showMessage: (NSString *) message {
    dispatch_time_t delay = dispatch_time(DISPATCH_TIME_NOW, NSEC_PER_SEC * 6.0);
    MBProgressHUD *messageHud = [MBProgressHUD showHUDAddedTo:self.view animated:YES];
    messageHud.labelText = message;
    messageHud.mode = MBProgressHUDModeText;
    dispatch_after(delay, dispatch_get_main_queue(), ^(void){
        messageHud.hidden = YES;
    });

}

- (void) waitForPost {
    dispatch_time_t delay = dispatch_time(DISPATCH_TIME_NOW, NSEC_PER_SEC * 2.0);
    dispatch_after(delay, dispatch_get_main_queue(), ^(void){
        if (self.gotPostAttempt) {
            self.gotPostAttempt = NO;
            self.webView.hidden = NO;
            self.mbProgressHUD.hidden = YES;
        }
    });
}

- (void) clearCache {
    [self.webView stringByEvaluatingJavaScriptFromString:@"document.body.innerHTML = \"\";"];
    NSHTTPCookieStorage *storage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    for (NSHTTPCookie *cookie in [storage cookies]) {
        [storage deleteCookie:cookie];
    }
    [[NSUserDefaults standardUserDefaults] synchronize];
}

- (NSString *) getResourceContents: (NSString *) resource {
    NSString *resourcePath = [NSString stringWithFormat:@"%@%@", [[NSBundle mainBundle] resourcePath], [NSString stringWithFormat:@"/%@", resource]];
    return [NSString stringWithContentsOfFile:resourcePath encoding:NSUTF8StringEncoding error:nil];
}

- (NSString *) getPropertyForModule: (NSString *) propertyName {
    for (ModuleProperty *property in self.module.properties) {
        NSString *name = property.name;
        if ([name isEqualToString:propertyName]) {
            return property.value;
            break;
        }
    }
    return @"";
}

@end
