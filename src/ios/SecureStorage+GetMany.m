//
//  SecureStorage+GetMultipleKeys.m
//  HelloCordova
//
//  Created by User on 21/3/17.
//
//

#import "SecureStorage+GetMany.h"
#import "SAMKeychain.h"

@implementation SecureStorage (GetMany)

-(void)getMany:(CDVInvokedUrlCommand*)command {
    
    NSString *service = [command argumentAtIndex:0];
    NSDictionary *keys = [command argumentAtIndex:1];
    
    [self.commandDelegate runInBackground:^{
        NSError *error;
        
        SAMKeychainQuery *query = [[SAMKeychainQuery alloc] init];
        query.service = service;
        
        NSMutableDictionary *values = [NSMutableDictionary new];
        for (NSString *key in keys) {
            query.account = key;
            if ([query fetch:&error]) {
                [values setValue:query.password forKey:key];
            }
        }
        
    
        CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:values];
        [self.commandDelegate sendPluginResult:commandResult callbackId:command.callbackId];
        
    }];
}

@end
