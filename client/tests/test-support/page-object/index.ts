export const pageObject = {
  buttons: {
    settings:
      "body > ui-view > div > div.left-menu > ngb-main-toolbar > div > ul > li:nth-child(5) > ngb-main-settings > button",
  },
  windows: {
    settings: {
      container:
        "body > div.md-dialog-container.ng-scope > md-dialog > form > md-toolbar > div > h2",
      tabs: {
        alignments: {
          container:
            "md-tabs > md-tabs-wrapper > md-tabs-canvas > md-pagination-wrapper > md-tab-item:nth-child(2) > span",
          content: '[role="tabpanel"]',
          checkboxes: {
            downsampling: '[aria-label="Downsample reads"]',
          },
          buttons: {
            save:
              "body > div.md-dialog-container.ng-scope > md-dialog > form > md-dialog-actions > button.md-primary.md-button.md-ink-ripple > span",
          },
        },
      },
    },
    main: {
      trackSettings: {
        groupBy: {
          container:
            "body > ui-view > div > div.golden-layout > ngb-golden-layout > div > div > div > div:nth-child(1) > div.lm_items > div > div > ngb-browser > ngb-tracks-view > div > div > div:nth-child(2) > ngb-track:nth-child(3) > div > div.ngb-track-header.layout-align-start-center.layout-row.flex-100 > ngb-track-settings > md-menu:nth-child(5) > button > a:nth-child(1)",
          options: {
            startLocation:
              "#menu_container_11 > md-menu-content > div:nth-child(2) > md-menu-item > button > span:nth-child(1)",
            insertSize:
              "#menu_container_11 > md-menu-content > div:nth-child(6) > md-menu-item > button > span:nth-child(1)",
          },
        },
      },
      canvas:
        "body > ui-view > div > div.golden-layout > ngb-golden-layout > div > div > div > div:nth-child(1) > div.lm_items > div > div > ngb-browser > ngb-tracks-view > div > div > div:nth-child(2) > ngb-track:nth-child(3) > div > div.ngb-track-renderer.js-ngb-render-container-target.flex-100 > div > canvas",
    },
  },
};
