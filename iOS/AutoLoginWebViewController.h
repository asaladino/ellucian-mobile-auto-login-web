#import <UIKit/UIKit.h>
#import "Module.h"
#import "MBProgressHUD.h"

@class Module;

@interface AutoLoginWebViewController : UIViewController <UIWebViewDelegate>

@property (strong, nonatomic) Module *module;
@property (weak, nonatomic) IBOutlet UIWebView *webView;
@property (strong, nonatomic) MBProgressHUD *mbProgressHUD;
@property (strong, nonatomic) UIBarButtonItem *rightBarButtonItem;


@property (strong, nonatomic) NSString *loginUrl;
@property (strong, nonatomic) NSString *usernameSelector;
@property (strong, nonatomic) NSString *passwordSelector;
@property (strong, nonatomic) NSString *formSelector;
@property (strong, nonatomic) NSString *extraFields;

@property (strong, nonatomic) NSString *redirectUrl;
@property (strong, nonatomic) NSString *redirectSelector;

@property (strong, nonatomic) NSString *cssUrl;
@property (strong, nonatomic) NSString *jsUrl;

@property BOOL allowInsecure;
@property BOOL gotPostAttempt;
@property (strong, nonatomic) NSString *username;
@property (strong, nonatomic) NSString *password;

- (void) loadLogin;
- (void) loadExternalResources;

@end
