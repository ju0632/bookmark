import Vue from "vue";
import {
  Button,
  FormModel,
  Input,
  Icon,
  message,
  Checkbox,
  Dropdown,
  Menu,
  Tree,
  Tooltip,
  Spin,
  notification,
  Empty,
  Modal,
  Radio,
  Upload,
  Popconfirm,
  AutoComplete,
  Select,
  Popover,
  Breadcrumb,
  Table
} from "ant-design-vue";
import App from "./App.vue";
import router from "./router";
import store from "./store";

const IconFont = Icon.createFromIconfontCN({
  scriptUrl: "//at.alicdn.com/t/c/font_1261825_v7m0rilm4hm.js"
});
Vue.use(Button);
Vue.use(FormModel);
Vue.use(Input);
Vue.component(Icon.name, Icon);
Vue.use(Checkbox);
Vue.use(Dropdown);
Vue.use(Menu);
Vue.use(Tree);
Vue.use(Tooltip);
Vue.use(Spin);
Vue.use(Empty);
Vue.use(Modal);
Vue.use(Radio);
Vue.use(Upload);
Vue.use(Popconfirm);
Vue.use(AutoComplete);
Vue.use(Select);
Vue.use(Popover);
Vue.use(Breadcrumb);
Vue.use(Table);
Vue.component("my-icon", IconFont);

Vue.prototype.$message = message;
Vue.prototype.$notification = notification;
Vue.prototype.$confirm = Modal.confirm;
Vue.config.productionTip = false;

window.vueInstance = new Vue({
  router,
  store,
  render: h => h(App)
}).$mount("#app");


