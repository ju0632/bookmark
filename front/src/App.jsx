import React, { Component } from "react";
import { Route, Switch, Redirect } from "react-router-dom";
import { message } from "antd";
import { withRouter } from "react-router-dom";
import { Provider } from "react-redux";
import ClipboardJS from "clipboard";
import store from "./redux";
import NotFound from "./pages/public/notFound/NotFound";
import UserSpace from "./pages/userSpace";
import SsoAuth from "./pages/userSpace/SsoAuth";

import Login from "./pages/public/Login";
import RegisterOrReset from "./pages/public/RegisterOrReset";
import EmailVerify from "./pages/public/EmailVerify";

import ManageOverview from "./pages/manage/OverView";
import Feedback from "./pages/manage/Feedback";

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {};
    window.reactHistory = this.props.history;
  }

  componentDidMount() {
    //初始化clipboard
    let clipboard = new ClipboardJS(".copy-to-board", {
      text: function(trigger) {
        return window.copyUrl;
      }
    });
    clipboard.on("success", function(e) {
      message.success("复制成功");
      e.clearSelection();
    });
  }

  render() {
    const mainStyle = {
      fontSize: "0.14rem"
    };
    return (
      <Provider store={store}>
        <div className="fullScreen" style={mainStyle}>
          <Switch>
            {/*书签管理页面*/}
            <Redirect exact path="/" to="/manage/overview" />
            <Route exact path="/manage/overview" component={ManageOverview} />
            {/* 反馈页面 */}
            <Route exact path="/manage/feedback" component={Feedback} />

            {/*个人中心页面*/}
            <Route exact path="/userSpace" component={UserSpace} />
            {/* 公共授权页面 */}
            <Route exact path="/userSpace/ssoAuth" component={SsoAuth} />
            {/* 公共页面 */}
            <Route exact path="/public/login" component={Login} />
            <Route exact path="/public/register" component={RegisterOrReset} />
            <Route
              exact
              path="/public/resetPassword"
              component={RegisterOrReset}
            />
            <Route exact path="/public/verifyEmail" component={EmailVerify} />
            <Route exact path="/404" component={NotFound} />
            {/* 当前面的路由都匹配不到时就会重定向到/404 */}
            <Redirect path="/" to="/404" />
          </Switch>
        </div>
      </Provider>
    );
  }
}

export default withRouter(App);
